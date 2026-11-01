# 1주차 진행 보고 - 김정수

- GitHub: @rlawjdtn1887-beep
- 기간: 2026-10-30 ~ 2026-11-01

## 이번 주 한 일

| 작업 | 결과 | 이슈/PR |
|------|------|---------|
| Pleos 샘플 앱 초기 세팅 (origin/upstream 분리, Vehicle SDK 2.3.2, distractionOptimized) | 완료 | - |
| 팀 README 팀원 표 작성 | 완료, merged | #7 |
| REGEN(회생제동 효율 피드백) 아이디어 후보 문서 작성 | 완료, merged | #8 |
| 공모전 요강 정리 (일정/심사기준/시상/제출 체크리스트) | 완료, merged | #9 |
| `PleosVehicleRepository` TODO 구현 (EvBattery/Brake/DrivingMode/HVAC/Seat/Odometer) | 완료, 에뮬레이터 실측 검증 | #11 |
| AndroidManifest에 Pleos Vehicle SDK 권한 누락으로 인한 앱 크래시 수정 | 완료 | #11 |
| HomeScreen.kt 컴파일 에러 수정 (destructuring + by 동시 사용 불가) | 완료 | #11 |
| AAOS CarPropertyManager로 외기온(outsideTempC) 추가 | 완료, 실측 확인 | #11 |

## 성과 / 배운 점

- Android Studio 자동완성 없이도 `Vehicle-2.3.2.aar`를 직접 디컴파일(`javap`)해서 각 도메인의 실제 메서드·리스너 시그니처를 확인하는 방법을 익힘. 덕분에 EvBattery/Brake/DrivingMode/HVAC/Seat/Odometer 6개 도메인의 TODO를 추측 없이 정확하게 채울 수 있었음
- 회생제동 에너지 → 절약 금액 계산 파이프라인(속도 변화 기반 `RegenEstimator` → `SavingsCalculator`)이 실제 에뮬레이터 주행으로 end-to-end 동작하는 걸 확인함 (예: 브레이크 1회로 0.65kWh 회수, 226원 절약으로 환산)
- Pleos Vehicle SDK에 없는 속도·기어·외기온은 AAOS `CarPropertyManager`로 보완하는 패턴을 확립함. 이 덕분에 REGEN 피치덱의 핵심 팁("히터 대신 시트 열선 켜면 +8km")이 죽은 로직이었던 걸 발견하고 고칠 수 있었음

## 막힌 점 / 도움 필요

- 이 에뮬레이터에 연결된 차량 프로필이 Pleos SDK의 Driving Mode(Eco/Normal/Sport) capability를 **"Unsupported"**로 보고함 (Vehicle Playground에서 확인). 다른 차량 프로필이나 실기기에서 재확인 필요
- HVAC/Seat 쪽 리스너는 등록 자체는 에러 없이 성공하는데 에뮬레이터에서 콜백이 전혀 안 들어옴. 원인 미파악 (capability 자체가 없는 건지, 콘솔 쪽 한계인지 추가 조사 필요)
- `BriefingScreen`의 목적지 거리가 NaviHelper 미연동으로 `42f` 하드코딩 상태. 다음 작업으로 예정

## 다음 주 계획

- [ ] NaviHelper SDK 연동해서 `BriefingScreen`의 실제 목적지 거리 반영
- [ ] HVAC/Seat 콜백 미수신 원인 조사
- [ ] PR #11 팀 리뷰 후 merge
