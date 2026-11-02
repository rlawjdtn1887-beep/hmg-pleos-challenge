# HMG Pleos App Market 공모전

현대자동차그룹 차량용 앱 개발 공모전(Pleos App Market) 참가 팀 저장소입니다.

> **아이템: EcoFit (REGEN)**
> 아이디어 후보는 [docs/01_ideas/](docs/01_ideas/)에서 제안하고 비교합니다.

## 다운로드

| 버전 | 날짜 | APK | 비고 |
|------|------|-----|------|
| <!-- v0.1.0 --> | <!-- 2026-11-XX --> | <!-- Release에 업로드 후 링크 --> | 첫 데모용 APK 빌드 후 작성 |

> APK는 저장소 코드가 아닌 [Releases](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/releases)에 첨부합니다.
> 최신 버전: [Latest Release](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/releases/latest)

## 앱 소개

**EcoFit** — 전기차 주행거리를 소프트웨어로 늘리는 EV 운전 코칭 앱

EV 운전자는 "한 번 충전으로 얼마나 갈 수 있는지" 늘 불안해하지만, 같은 차도 운전 습관에 따라 전비가 20~30% 차이 납니다. 계기판은 전비 숫자만 보여줄 뿐 "어떻게 운전해야 더 가는지"는 알려주지 않습니다.

EcoFit은 Pleos Vehicle SDK로 배터리·회생제동·공조 데이터를 실시간으로 읽어, 출발 전 / 주행 중 / 주행 후 매 순간 행동을 코칭합니다. 그리고 아낀 에너지를 "오늘 3,200원 아꼈어요"처럼 돈과 커피잔 수로 환산해, 전비 개선을 게임처럼 체감하게 만듭니다.

### 핵심 기능

- **실시간 회생 게이지** — 브레이크를 밟을 때마다 회수되는 에너지를 즉시 확인 (실측: 브레이크 1회 0.65kWh / 226원 회수)
- **상황별 코칭** — "외기 12°C 미만이면 히터 대신 시트 열선 켜면 +8km" 같은 구체적 제안
- **돈 환산 리포트** — 아낀 전기를 원화·커피잔·추가 주행거리로 변환해 홈 화면에 노출

### Pleos SDK 활용

EvBattery(배터리·전비) · Brake(회생제동) · HVAC(공조) · Odometer(주행거리) · DrivingMode를 직접 사용하며, SDK에 없는 속도·기어·외기온은 AAOS CarPropertyManager로 보완했습니다.

## 스크린샷 / 시연

| 홈 | 출발 전 | 주행 중 | 리포트 |
|------|------|------|------|
| ![홈](docs/03_design/assets/screen-home.png) | ![출발 전](docs/03_design/assets/screen-briefing.png) | ![주행 중](docs/03_design/assets/screen-drive.png) | ![리포트](docs/03_design/assets/screen-report.png) |

> 시연영상: [docs/05_submission/demo-video/](docs/05_submission/demo-video/)에 추가 예정

## 팀원

| 이름 | 역할 | GitHub |
|------|------|--------|
| 김정수 | 개발 | [@rlawjdtn1887-beep](https://github.com/rlawjdtn1887-beep) |
| 김다희 | 기획, 발표 | [@darongki37](https://github.com/darongki37) |
| 박찬서 | 개발 | [@Chanseo02](https://github.com/Chanseo02) |

## 팀원별 기여

> 각자 맡은 부분과 주요 PR을 적습니다. 주차별 상세 내용은 [docs/progress/](docs/progress/)에 있습니다.

| 이름 | 담당 | 주요 기여 | 주요 PR |
|------|------|-----------|---------|
| @rlawjdtn1887-beep | Pleos SDK 연동 | `PleosVehicleRepository`(EvBattery/Brake/DrivingMode/HVAC/Seat/Odometer) 구현, NaviHelper 연동(출발 전 목적지 거리·도착 시간), 매니페스트 권한 누락 크래시 수정 | [#11](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/11), [#15](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/15), [#16](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/16) |
| @darongki37 | 기획, 발표 | REGEN 아이디어 기획·검증, 공모전 서류·발표자료 준비 | [#8](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/8) |
| @Chanseo02 | 앱 구조·UI | EcoFit 프로젝트 초기 세팅(DB/도메인/화면 4개), 타일 기반 UI 리디자인, 회생제동 금액 팝업, 급제동 판정 로직 고도화 | [#10](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/10), [#17](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pull/17) |

전체 기여 내역: [Contributors](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/graphs/contributors) · [병합된 PR](https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/pulls?q=is%3Apr+is%3Amerged)

## 폴더 구조

```
.
├── README.md               # 저장소 소개 (이 문서)
├── CONTRIBUTING.md         # 브랜치·커밋·PR 규칙
├── app/                    # Android Studio 프로젝트 (아이템 확정 후 생성)
├── docs/
│   ├── GUIDE.md            # 팀 GitHub 사용 설명서
│   ├── 00_contest-info/    # 공모전 요강·일정·심사기준, 제출 조건 체크리스트
│   ├── 01_ideas/           # 아이디어 후보 (1개당 1파일) + 후보 비교표
│   ├── 02_research/        # UX 리서치·레퍼런스 정리
│   ├── 03_design/          # 화면 흐름·와이어프레임 (이미지는 assets/)
│   ├── 04_roadmap/         # 주차별 로드맵
│   ├── 05_submission/      # 발표자료·시연영상
│   ├── progress/           # 주간 진행 보고 (팀원별 성과)
│   └── meeting-notes/      # 회의록 (YYYY-MM-DD.md)
└── .github/                # 이슈·PR 템플릿
```

## 개발 환경

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin 2.0.0 |
| UI | Jetpack Compose (BOM 2024.04.01), Navigation Compose 2.8.9 |
| 빌드 | AGP 8.8.2, Gradle |
| 로컬 저장소 | Room 2.6.1 |
| 비동기 | Kotlinx Coroutines 1.8.1 |
| Pleos SDK | Vehicle SDK 2.3.2, NaviHelper SDK 3.0.5 |
| IDE | Android Studio (차량용 에뮬레이터) |
| 테스트 환경 | Pleos Playground (Pleos Connect v2.1.0) |
| 협업 | GitHub Issues / Projects / Discussions |

## 일정

| 날짜 (KST) | 내용 | 비고 |
|------|------|------|
| 2026.10.21(수) | 공고·접수 시작 | |
| 2026.10.28(수) | 온라인 설명회 | |
| **2026.11.15(일)** | **접수 마감** | |
| 2026.11.23(월) | 1차 서류심사 결과 발표 | |
| 2026.12.03(목)~04(금) | 2차 최종 PT 심사 | 오프라인, 실제 동작 앱 필요 |
| 2026.12.18(금) | 최종 선정 결과 발표 | |

> 세부 내용은 [docs/00_contest-info/](docs/00_contest-info/)의 공모전 요강 참고.

## 협업 방법

> 처음이라면 [팀 GitHub 사용 설명서](docs/GUIDE.md)부터 읽어주세요.

- 할 일은 **Issues**로 만들고 **Projects 보드("공모전 준비")** 에서 관리합니다.
- 자유로운 아이디어 토론은 **Discussions**를 사용합니다.
- 매주 각자 진행 상황을 [docs/progress/](docs/progress/)에 기록합니다.
- 작업 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.
