package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.CheckInHistoryDto;
import com.wiinvent.lotus.checkin.dto.UserDto;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MessageSource messageSource;
    private final RedissonClient redissonClient;
    private final CheckInValidateHelper checkInValidateHelper;
    private final CheckInHistoryRepository checkInHistoryRepository;

    private final RewardConfigService rewardConfigService;

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

        validateCheckIn(userId, locale, dateCheckIn, checkInBucket);
        RLock lock = redissonClient.getLock("checkInLock:" + userId);

        try {
            if (lock.tryLock(5,10, TimeUnit.SECONDS)) {
                try {

                    UserEntity userEntity = getUserEntity(userId,locale);

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
                    setCacheUserProfile(redissonClient.getBucket(CacheKeys.USER_PROFILE.buildKey(userId)),
                            userMapper.toDto(userEntity));

                    CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
                    checkInHistoryEntity.setUserId(userId);
                    checkInHistoryEntity.setAmount(rewardConfigs.getOrDefault(turnInMonth.size() + 1, 0));
                    checkInHistoryEntity.setCheckInDate(dateCheckIn);
                    checkInHistoryEntity.setReason(ReasonCheckInEnum.check_in.name());
                    checkInHistoryRepository.save(checkInHistoryEntity);

                    addCacheCheckInBucket(userId, checkInBucket);
                }
                finally {
                    lock.unlock();
                }
            }
            else {
                throw new RuntimeException(messageSource.getMessage(LocaleKey.CHECK_IN_LOCK_FAILED, null, locale));
            }

        } catch (Exception e) {
            checkInBucket.delete();
            throw e;
        }

    }

    private List<CheckInHistoryEntity> getCheckInByDateRange(long userId,
                                                             LocalDate startDate,
                                                             LocalDate endDate,String reason) {
        String cacheKeyDateRange = String.format("checkInRange:%d:%s:%s:%s",
                userId, ReasonCheckInEnum.check_in.name(),
                startDate,
                endDate);

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
            if(!checkInBucket.isExists()){
                addCacheCheckInBucket(userId, checkInBucket);
            }
            throw new Exception(messageSource.getMessage(LocaleKey.CHECK_IN_ALREADY_MARKED_TODAY, null, locale));
        }
    }

    public List<CheckInHistoryDto> getCheckInStatusById(long userId, LocalDate startDate,
                                                        LocalDate endDate) {

        List<CheckInHistoryEntity> checkInHistoryEntities =
                checkInHistoryRepository
                        .findByUserIdAndReasonAndCheckInDateGreaterThanEqualAndCheckInDateLessThanEqual(
                                userId, ReasonCheckInEnum.check_in.name(), startDate, endDate);
        Set<LocalDate> checkInDates = checkInHistoryEntities.stream()
                .map(CheckInHistoryEntity::getCheckInDate)
                .collect(Collectors.toSet());

        return Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                .map(currentDate -> CheckInHistoryDto.builder()
                        .checkInDate(currentDate)
                        .isCheckedIn(checkInDates.contains(currentDate))
                        .build())
                .collect(Collectors.toList());

    }
    @Transactional
    public void subtractPoints(long userId, CheckInHistoryDto checkInHistoryDto, Locale locale) {
        UserEntity userEntity = getUserEntity(userId,locale);

        userEntity.setLotusPoints(userEntity.getLotusPoints() + checkInHistoryDto.getAmount());
        userRepository.save(userEntity);

        setCacheUserProfile(redissonClient.getBucket(CacheKeys.USER_PROFILE.buildKey(userId)),
                userMapper.toDto(userEntity));

        CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
        checkInHistoryEntity.setUserId(userId);
        checkInHistoryEntity.setAmount(checkInHistoryDto.getAmount());
        checkInHistoryEntity.setCheckInDate(checkInHistoryDto.getCheckInDate() != null ?
                checkInHistoryDto.getCheckInDate() : LocalDate.now());
        checkInHistoryEntity.setReason(ReasonCheckInEnum.deducted.name());
        checkInHistoryRepository.save(checkInHistoryEntity);
    }

    private void addCacheCheckInBucket(long userId, RBucket<String> checkInBucket) {
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
}
