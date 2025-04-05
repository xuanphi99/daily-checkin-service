package com.wiinvent.lotus.checkin.service;

import com.wiinvent.lotus.checkin.dto.UserDto;
import com.wiinvent.lotus.checkin.entity.UserEntity;
import com.wiinvent.lotus.checkin.mapper.UserMapper;
import com.wiinvent.lotus.checkin.repository.CheckInHistoryRepository;
import com.wiinvent.lotus.checkin.repository.UserRepository;
import com.wiinvent.lotus.checkin.util.CacheKeys;
import com.wiinvent.lotus.checkin.util.CheckInValidateHelper;
import com.wiinvent.lotus.checkin.util.LocaleKey;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MessageSource messageSource;
    private final RedissonClient redissonClient;
    private final CheckInValidateHelper checkInValidateHelper;
    private final CheckInHistoryRepository checkInHistoryRepository;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       MessageSource messageSource,
                       RedissonClient redissonClient,
                       CheckInValidateHelper checkInValidateHelper,
                       CheckInHistoryRepository checkInHistoryRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.messageSource = messageSource;
        this.redissonClient = redissonClient;
        this.checkInValidateHelper = checkInValidateHelper;
        this.checkInHistoryRepository = checkInHistoryRepository;
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
        // Tạo key Redis để lưu trạng thái điểm danh
        RBucket<String> checkInBucket = redissonClient.getBucket(CacheKeys.USER_CHECK_IN.buildKey(userId));

        // Kiểm tra thời gian điểm danh có hợp lệ không
        if (!checkInValidateHelper.isTimeInRange()) {
            throw new Exception("Invalid check-in time");
        }

        // Kiểm tra nếu Redis có key này, tức là đã điểm danh
        else if (checkInBucket.isExists()) {
            throw new Exception("Check-in already marked today (from Redis)");
        }

        // Nếu Redis không có key, fallback vào DB để kiểm tra
        else if (!checkInHistoryRepository.findByUserIdAndCheckInDate(userId, dateCheckIn).isPresent()) {
            throw new Exception("Check-in already marked today (from DB)");
        }

        // Nếu không có trong cả Redis và DB, thực hiện điểm danh
        try {
            // Lấy thông tin người dùng từ DB
            UserEntity userEntity = userRepository.findById(userId).orElseThrow(() ->
                    new RuntimeException("User not found"));

            // Nếu trong tháng vượt quá lượt điểm danh (turn > 7)
//            if (userEntity.getTurn() > 7) {
//                throw new RuntimeException("User has exceeded the allowed number of check-ins for this month.");
//            }
//
//            // Cập nhật lượt điểm danh cho người dùng
//            userEntity.setTurn(userEntity.getTurn() + 1);
//            userRepository.save(userEntity);
//
//            // Lưu lịch sử điểm danh vào DB
//            TurnHistoryEntity turnHistoryEntity = new TurnHistoryEntity();
//            turnHistoryEntity.setUserId(userId);
//            turnHistoryEntity.setAmount(1);
//            turnHistoryEntity.setBalance(balance);
//            turnHistoryEntity.setCreateAt(new Date());
//            turnHistoryRepository.save(turnHistoryEntity);
//
//            // Lưu trạng thái điểm danh vào Redis
//            checkInBucket.set(CacheKeys.USER_CHECK_IN.buildKey(userId));
//            checkInBucket.expire(checkInService.getExpiryTime().toInstant()); // Cài đặt hết hạn key Redis sau 24h

        } catch (Exception e) {
            // Xử lý lỗi và xóa Redis key nếu có lỗi
            checkInBucket.delete();
            throw e;
        }

    }
}
