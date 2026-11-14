# REGEN — 회생제동 효율 피드백 앱

- 제안자: @rlawjdtn1887-beep
- 작성일: 2026-10-30

## 문제

전기차 운전자는 가속페달에서 발을 떼거나 브레이크를 밟을 때 회생제동으로 배터리를 얼마나
충전했는지 알 방법이 없다. 계기판에는 SOC(배터리 잔량, %)만 정수로 표시될 뿐, 특정 감속
구간에서 얼마나 효율적으로 에너지를 회수했는지에 대한 즉각적인 피드백이 없어 운전 습관을
개선하기 어렵다.

## 타깃

전기차(EV) 운전자, 특히 주행 효율(전비)을 신경 쓰는 운전자.

## 핵심 기능

1. 실시간 회생제동 피드백 — 감속/브레이크 구간에서 회수된 에너지를 시각적으로 표시
2. 주행 중 순간 전비 및 SOC 변화 추이 표시
3. 주행 종료 후 요약 리포트 — 오늘 회생제동으로 아낀 배터리량을 주행거리로 환산해 보여줌

## Pleos SDK 활용

- `EvBatteryStateOfCharge` capability 확인 → get → register/unregister 흐름으로 SOC 구독
  (Vehicle SDK 2.3.2, 오늘 샘플 앱에서 테스트 완료)
- AAOS 차량 신호 연동 필요: `PERF_VEHICLE_SPEED`(차속), `GEAR_SELECTION`(기어) — 샘플 앱에는
  없어 REGEN 전용 프로젝트에서 직접 연결해야 함
- Brake 페달 값, Odometer(트립 거리/시간) — 감속 구간과 회생량을 매칭하는 데 활용 검토

## 구현 난이도

- 난이도: 중
- 이유: SDK 연동 자체(SOC 구독)는 이미 검증됨. 다만 SOC가 정수(%) 단위라 감속 한 번의
  회생량을 SOC만으로 정밀 측정하기 어려워 물리 모델 기반 추정이 필요함.
- 리스크: SOC 갱신 주기와 해상도가 낮아 실시간 피드백의 정확도가 떨어질 수 있음. 차속/기어
  신호를 안정적으로 받아오는 AAOS 연동 검증이 선행되어야 함.

## 참고 자료

- 2026-10-30 Pleos 샘플 앱 테스트 기록(dev/sample-test 브랜치) — Vehicle SDK 2.3.2로
  `checkEvBatteryStateOfChargeCapabilities`, `getEvBatteryStateOfCharge`,
  `registerEvBatteryStateOfCharge` 호출 검증 완료
