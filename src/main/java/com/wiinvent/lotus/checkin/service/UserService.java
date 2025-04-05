package com.wiinvent.lotus.checkin.service;

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
import org.redisson.api.RedissonClient;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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

    public UserDto createUser(UserDto userDto) {

        UserEntity user = userMapper.toEntity(userDto);
        UserEntity userEntity = userRepository.save(user);
        return userMapper.toDto(userEntity);
    }

    public ResponseEntity<?> getUserById(long userId, Locale locale) {
        Optional<UserEntity> userEntity = userRepository.findById(userId);
        if (userEntity.isPresent()) {
            return ResponseEntity.ok(userMapper.toDto(userEntity.get()));
        }
        String message = messageSource.getMessage(LocaleKey.USER_NOT_FOUND, null, locale);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public void checkInByUserId(long userId, Locale locale) throws Exception {
        LocalDate dateCheckIn = LocalDate.now();
        RBucket<String> checkInBucket = redissonClient.getBucket(CacheKeys.USER_CHECK_IN.buildKey(userId));

        if (!checkInValidateHelper.isTimeInRange()) {
            throw new Exception(messageSource.getMessage(LocaleKey.INVALID_CHECK_IN_TIME, null, locale));
        }

        else if (checkInBucket.isExists()) {
            throw new Exception(messageSource.getMessage(LocaleKey.CHECK_IN_ALREADY_MARKED_TODAY, null, locale));
        }

        else if (checkInHistoryRepository.findByUserIdAndCheckInDate(userId, dateCheckIn).isPresent()) {
            addCacheCheckInBucket(userId, checkInBucket);
            throw new Exception(messageSource.getMessage(LocaleKey.CHECK_IN_ALREADY_MARKED_TODAY, null, locale));
        }

        try {

            UserEntity userEntity = userRepository.findById(userId).orElseThrow(() ->
                    new RuntimeException(messageSource.getMessage(LocaleKey.USER_NOT_FOUND, null, locale)));

            List<CheckInHistoryEntity> turnInMonth =  checkInHistoryRepository
                    .findByCheckInDateGreaterThanEqualAndCheckInDateLessThanEqual(
                            dateCheckIn.withDayOfMonth(1),
                            dateCheckIn.with(TemporalAdjusters.lastDayOfMonth()));

            HashMap<Integer,Integer> rewardConfigs  = rewardConfigService.findAllConfig();

            if (turnInMonth.size() > rewardConfigs.size()) {
                throw new RuntimeException(messageSource.getMessage(LocaleKey.USER_EXCEEDED_CHECK_INS, null, locale));
            }
            userEntity.setLotusPoints(userEntity.getLotusPoints() + rewardConfigs.getOrDefault(turnInMonth.size() + 1,0));
            userRepository.save(userEntity);

            CheckInHistoryEntity checkInHistoryEntity = new CheckInHistoryEntity();
            checkInHistoryEntity.setUserId(userId);
            checkInHistoryEntity.setAmount(rewardConfigs.getOrDefault(turnInMonth.size() + 1,0));
            checkInHistoryEntity.setCheckInDate(dateCheckIn);
            checkInHistoryEntity.setReason(ReasonCheckInEnum.check_in.name());
            checkInHistoryRepository.save(checkInHistoryEntity);

            addCacheCheckInBucket(userId, checkInBucket);

        } catch (Exception e) {
            checkInBucket.delete();
            throw e;
        }

    }

    private void addCacheCheckInBucket(long userId, RBucket<String> checkInBucket) {
        checkInBucket.set(CacheKeys.USER_CHECK_IN.buildKey(userId));
        checkInBucket.expire(checkInValidateHelper.getExpiryTime().toInstant());
    }
}
