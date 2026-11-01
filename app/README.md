# EcoFit

EV·하이브리드 운전 습관을 차량 데이터로 읽어 코칭하고, 아낀 에너지를 돈으로 보여주는 Pleos Connect 앱.

팀 Back to the Future · 현대자동차그룹 차량용 앱 개발 공모전

## 실행 방법

1. Android Studio → Open → 저장소의 `app/` 폴더 선택
2. Pleos Connect v2.1.0 AVD 선택 후 Run
3. 차량 데이터 조작: `adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088`

## 테스트 환경

- Pleos Connect Emulator v2.1.0 (API 34)
- Vehicle SDK 2.3.2, NaviHelper SDK 3.0.5
- Android Studio 2026.x, JDK 17

## 가짜 데이터 모드

`EcoFitApp.USE_FAKE_VEHICLE = true` 면 SDK 없이 90초 주기 가짜 주행이 돈다. 에뮬레이터에서 실데이터로 볼 때는 `false`.

## 구조

```
com.ecofit.app
├── EcoFitApp.kt                 앱 진입점. 레포·DB·레코더 연결
├── MainActivity.kt              네비게이션 레일 + 4개 화면
├── data
│   ├── model
│   │   ├── VehicleSnapshot.kt   차량 상태 한 시점 (정수 ↔ 찬서 인터페이스)
│   │   └── TripData.kt          주행 한 건 결과
│   ├── vehicle
│   │   ├── VehicleRepository.kt        인터페이스
│   │   ├── PleosVehicleRepository.kt   실제 SDK 연동 (정수)
│   │   ├── CarPropertySource.kt        기어·차속 (AAOS CarPropertyManager)
│   │   └── FakeVehicleRepository.kt    가짜 시뮬레이터 (찬서)
│   └── local                    Room DB
├── domain
│   ├── TripRecorder.kt          기어 D→P 감지해 TripData 생성, 주행 중 실시간 값
│   ├── RegenEstimator.kt        차속 변화로 회생량 추정
│   └── SavingsCalculator.kt     kWh → 원·km·커피 환산
└── ui
    ├── home / briefing / drive / report
    └── theme
```

## 회생량 계산

SoC가 정수(%)로만 와서 감속 한 번의 회생량은 SoC로 잴 수 없다. 감속 구간의 운동에너지 감소에서 구름·공기 저항 손실을 빼고 회생 효율(0.65)을 곱해 추정한다. 차량 질량 2,050kg, 최대 회생 100kW 기준. 60km/h에서 정지하면 약 50Wh.

## 권한

- `android.car.permission.CAR_SPEED` (런타임 요청): 차속
- `android.car.permission.CAR_POWERTRAIN`: 기어

## 역할

- 정수: `PleosVehicleRepository` TODO 채우기 (도메인별 register), NaviHelper 연동
- 찬서: 화면·계산 로직·DB

## APK

`apk/BackToTheFuture_EcoFit_release.apk` (추가 예정)
