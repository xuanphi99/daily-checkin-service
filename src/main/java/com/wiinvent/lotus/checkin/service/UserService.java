package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.config.LiquibaseChecksumClearConfig;
import com.wiinvent.lotus.checkin.dto.*;
import com.wiinvent.lotus.checkin.entity.CheckInHistoryEntity;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import com.wiinvent.lotus.checkin.mapper.UserMapper;
import com.wiinvent.lotus.checkin.repository.CheckInHistoryRepository;
import com.wiinvent.lotus.checkin.repository.UserRepository;
import com.wiinvent.lotus.checkin.util.CacheKeys;
import com.wiinvent.lotus.checkin.util.CheckInValidateHelper;
import com.wiinvent.lotus.checkin.util.LocaleKey;
import com.wiinvent.lotus.checkin.util.ReasonCheckInEnum;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MessageSource messageSource;
    private final RedissonClient redissonClient;
    private final CheckInValidateHelper checkInValidateHelper;
    private final CheckInHistoryRepository checkInHistoryRepository;

    private final RewardConfigService rewardConfigService;

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       MessageSource messageSource,
                       RedissonClient redissonClient,
                       CheckInValidateHelper checkInValidateHelper,
                       CheckInHistoryRepository checkInHistoryRepository,
                       RewardConfigService rewardConfigService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.messageSource = messageSource;
        this.redissonClient = redissonClient;
        this.checkInValidateHelper = checkInValidateHelper;
        this.checkInHistoryRepository = checkInHistoryRepository;
        this.rewardConfigService = rewardConfigService;
    }

    @Transactional
    public UserDto createUser(UserDto userDto) {

        UserEntity user = userMapper.toEntity(userDto);
        UserEntity userEntity = userRepository.save(user);
        return userMapper.toDto(userEntity);
    }

    public ResponseEntity<?> getUserById(long userId, Locale locale) {
        RBucket<UserDto> userBucket = redissonClient.getBucket(CacheKeys.USER_PROFILE.buildKey(userId));
        UserDto userDto = userBucket.get();
        if (userDto != null) {
            return ResponseEntity.ok(userDto);
        }
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (userEntity.isPresent()) {
            UserDto dto = userMapper.toDto(userEntity.get());
            setCacheUserProfile(userBucket, dto);
            return ResponseEntity.ok(dto);
        }
        String message = messageSource.getMessage(LocaleKey.USER_NOT_FOUND, null, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public void checkInByUserId(long userId, Locale locale) throws Exception {
        LocalDate dateCheckIn = LocalDate.now();
        RBucket<String> checkInBucket = redissonClient.getBucket(CacheKeys.USER_CHECK_IN.buildKey(userId));
        RBucket<UserDto> userBucket = redissonClient.getBucket(CacheKeys.USER_PROFILE.buildKey(userId));

        validateCheckIn(userId, locale, dateCheckIn, checkInBucket);
        RLock lock = redissonClient.getLock("checkInLock:" + userId);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {

                    UserEntity userEntity = getUserEntity(userId, locale);

                    List<CheckInHistoryEntity> turnInMonth = getCheckInByDateRange(userId,
                            dateCheckIn.withDayOfMonth(1),
                            dateCheckIn.with(TemporalAdjusters.lastDayOfMonth()),
                            ReasonCheckInEnum.check_in.name());

                    HashMap<Integer, Integer> rewardConfigs = rewardConfigService.findAllConfig();

                    if (turnInMonth.size() >= rewardConfigs.size()) {
                        throw new RuntimeException(messageSource.getMessage(LocaleKey.USER_EXCEEDED_CHECK_INS, null, locale));
                    }


                    userEntity.setLotusPoints(userEntity.getLotusPoints() +
                            rewardConfigs.getOrDefault(turnInMonth.size() + 1, 0));
                    userRepository.save(userEntity);

                    CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
                    checkInHistoryEntity.setUserId(userId);
                    checkInHistoryEntity.setAmount(rewardConfigs.getOrDefault(turnInMonth.size() + 1, 0));
                    checkInHistoryEntity.setCheckInDate(dateCheckIn);
                    checkInHistoryEntity.setReason(ReasonCheckInEnum.check_in.name());
                    checkInHistoryRepository.save(checkInHistoryEntity);

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            setCacheUserProfile(userBucket, userMapper.toDto(userEntity));
                            setCacheCheckInBucket(userId, checkInBucket);
                            setCacheCheckInHistory(checkInHistoryEntity,
                                    redissonClient,
                                    cacheKeyDateRange(userId, dateCheckIn));
                            updateCacheHistoryPagination(redissonClient, userId);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                                logger.debug("Transaction committed.");
                            } else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                                logger.debug("Transaction rolled back.");
                            }
                        }
                    });

                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException(messageSource.getMessage(LocaleKey.CHECK_IN_LOCK_FAILED, null, locale));
            }

        } catch (Exception e) {
            userBucket.delete();
            checkInBucket.delete();
            throw e;
        }

    }

    private String cacheKeyDateRange(long userId, LocalDate checkInDate) {
        return String.format("checkInRange:%d:%s:%s:%s",
                userId, ReasonCheckInEnum.check_in.name(),
                checkInDate.withDayOfMonth(1),
                checkInDate.with(TemporalAdjusters.lastDayOfMonth()));

    }


    private List<CheckInHistoryEntity> getCheckInByDateRange(long userId,
                                                             LocalDate startDate,
                                                             LocalDate endDate, String reason) {
        String cacheKeyDateRange = cacheKeyDateRange(userId, startDate);

        RBucket<List<CheckInHistoryEntity>> bucket = redissonClient.getBucket(cacheKeyDateRange);

        if (bucket.isExists()) {
            return bucket.get();
        }

        List<CheckInHistoryEntity> result = checkInHistoryRepository
                .findByUserIdAndReasonAndCheckInDateGreaterThanEqualAndCheckInDateLessThanEqual(
                        userId, reason, startDate, endDate);

        bucket.set(result);
        bucket.expire(checkInValidateHelper.getExpiryTime().toInstant());

        return result;
    }

    private void validateCheckIn(long userId, Locale locale, LocalDate dateCheckIn, RBucket<String> checkInBucket) throws Exception {
        if (!checkInValidateHelper.isTimeInRange()) {
            throw new Exception(messageSource.getMessage(LocaleKey.INVALID_CHECK_IN_TIME, null, locale));
        }
        if (checkInBucket.isExists() || checkInHistoryRepository.findByUserIdAndCheckInDate(userId, dateCheckIn).isPresent()) {
            if (!checkInBucket.isExists()) {
                setCacheCheckInBucket(userId, checkInBucket);
            }
            throw new Exception(messageSource.getMessage(LocaleKey.CHECK_IN_ALREADY_MARKED_TODAY, null, locale));
        }
    }

    public List<CheckInStatus> getCheckInStatusById(long userId) {
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        HashMap<Integer, Integer> rewardConfigs = rewardConfigService.findAllConfig();

        List<CheckInHistoryEntity> checkInHistoryEntities =
                getCheckInByDateRange(userId, startDate, endDate, ReasonCheckInEnum.check_in.name());

        int checkInCount = checkInHistoryEntities.size();
        int rewardCount = rewardConfigs.size();

        return IntStream.range(0, rewardCount)
                .mapToObj(i -> CheckInStatus.builder()
                        .day(i+1)
                        .isCheckedIn(i < checkInCount)
                        .build())
                .collect(Collectors.toList());

    }

    @Transactional
    public void subtractPoints(long userId, CheckInHistoryDto checkInHistoryDto, Locale locale) {
        UserEntity userEntity = getUserEntity(userId, locale);

        userEntity.setLotusPoints(userEntity.getLotusPoints() + checkInHistoryDto.getAmount());
        userRepository.save(userEntity);

        setCacheUserProfile(redissonClient.getBucket(CacheKeys.USER_PROFILE.buildKey(userId)),
                userMapper.toDto(userEntity));

        CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
        checkInHistoryEntity.setUserId(userId);
        checkInHistoryEntity.setAmount(checkInHistoryDto.getAmount());
        checkInHistoryEntity.setCheckInDate(checkInHistoryDto.getCheckInDate() != null ?
                checkInHistoryDto.getCheckInDate() : java.time.LocalDate.now());
        checkInHistoryEntity.setReason(ReasonCheckInEnum.deducted.name());
        checkInHistoryRepository.save(checkInHistoryEntity);
    }

    private void setCacheCheckInBucket(long userId, RBucket<String> checkInBucket) {
        checkInBucket.set(CacheKeys.USER_CHECK_IN.buildKey(userId));
        checkInBucket.expire(checkInValidateHelper.getExpiryTime().toInstant());
    }

    private void setCacheUserProfile(RBucket<UserDto> userBucket, UserDto dto) {
        userBucket.set(dto);
        userBucket.expire(checkInValidateHelper.getExpiryTime().toInstant());
    }

    private UserEntity getUserEntity(long userId, Locale locale) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage(LocaleKey.USER_NOT_FOUND, null, locale)));
    }

    private void setCacheCheckInHistory(CheckInHistoryEntity entity, RedissonClient redissonClient, String cacheKeyDateRange) {
        RBucket<List<CheckInHistoryEntity>> bucket = redissonClient.getBucket(cacheKeyDateRange);

        List<CheckInHistoryEntity> checkInHistoryEntities = Optional.ofNullable(bucket.get()).orElse(new ArrayList<>());

        checkInHistoryEntities.add(entity);

        bucket.set(checkInHistoryEntities);

        bucket.expire(checkInValidateHelper.getExpiryTime().toInstant());
    }

    private static void updateCacheHistoryPagination(RedissonClient redissonClient, long userId) {
        RBucket<String> keyPage = redissonClient.getBucket(PaginationRequest.buildCacheKey(userId));
        if (keyPage.isExists()) {
            String page = keyPage.get();
            RKeys rKeys = redissonClient.getKeys();
            Iterable<String> keys = rKeys.getKeys();
            keys.forEach(key -> {
                if (key.contains("pagination")) {
                    RBucket<Object> removeBucket = redissonClient.getBucket(PaginationRequest.buildCacheKey(userId) + page);
                    removeBucket.delete();
                }
            });
        }
    }


}
