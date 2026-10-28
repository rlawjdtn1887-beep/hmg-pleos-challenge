# 팀 GitHub 사용 설명서

> 우리 팀은 GitHub를 **아이디어 저장소 + 소통 공간 + 작업 관리 도구**로 씁니다.
> 카톡에서 나온 이야기도 결정되면 반드시 GitHub에 남깁니다. **GitHub에 없으면 없는 것**이 원칙입니다.

---

## 1. 한눈에 보기: 무엇을 어디에?

| 하고 싶은 일 | 쓰는 곳 | 예시 |
|---|---|---|
| 아이디어를 가볍게 던지고 의견 듣기 | **Discussions → 💡 Ideas** | "운전 중 ○○하면 어떨까?" |
| 궁금한 것 질문하기 | **Discussions → 🙏 Q&A** | "에뮬레이터 실행이 안 돼요" |
| 투표로 정하기 | **Discussions → 🗳️ Polls** | "회의 요일 투표" |
| 공지하기 | **Discussions → 📣 Announcements** | "이번 주 회의 일정" |
| 만든 결과물 자랑·공유 | **Discussions → 🙌 Show and tell** | "첫 화면 시안 나왔어요" |
| 해야 할 일 등록 | **Issues** | "공모전 요강 정리" |
| 작업 진행 상황 보기 | **Projects 보드 "공모전 준비"** | Todo → In Progress → Review → Done |
| 결과물(문서·코드) 올리기 | **Pull Request** | 아이디어 문서, 회의록, 앱 코드 |
| 확정된 내용 보관 | **저장소 `docs/` 폴더** | 요강, 아이디어, 리서치, 회의록 |
| 매주 내가 한 일 기록 | **`docs/progress/`** | `week01-내아이디.md` |

**쉽게 구분하기**
- **Discussions** = 단톡방 (자유롭게 이야기)
- **Issues** = 할 일 카드 (담당자와 완료 조건이 있음)
- **docs/ 폴더** = 정리된 자료실 (확정된 내용만)

---

## 2. 바로가기

| 공간 | 링크 |
|---|---|
| 저장소 | https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge |
| Discussions | https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/discussions |
| Issues | https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/issues |
| 보드 | https://github.com/users/rlawjdtn1887-beep/projects/1 |
| Releases (APK) | https://github.com/rlawjdtn1887-beep/hmg-pleos-challenge/releases |

---

## 3. 아이디어 흐름: 떠올림 → 토론 → 정리 → 결정

```
① Discussions/Ideas에 글 작성 (양식 자동 제공)
        ↓  팀원 댓글·👍 반응
② 괜찮은 아이디어 → docs/01_ideas/idea-번호-이름.md 로 정리 (PR)
        ↓
③ docs/01_ideas/comparison.md 비교표에 점수 매기기
        ↓
④ 회의에서 결정 → 회의록에 기록 → README "아이템" 업데이트
```

### ① Discussions에 아이디어 올리기
1. **Discussions** 탭 → **New discussion** → **💡 Ideas** 선택
2. 양식(문제 / 타깃 / 핵심 기능 / Pleos SDK 활용 / 난이도)을 채웁니다. 다 몰라도 괜찮고, **한 줄 요약만 있어도 올립니다.**
3. **Start discussion**

### 다른 사람 아이디어에 반응하기
- 좋으면 👍, 아주 좋으면 🚀 반응을 남깁니다. 반응 수가 곧 관심도입니다.
- 댓글은 **"좋아요"보다 구체적으로** 씁니다. 예: "타깃을 ○○로 좁히면 SDK의 ○○ 기능을 쓸 수 있을 듯"
- 특정 팀원 의견이 필요하면 `@아이디`로 부릅니다. 그 사람에게 알림이 갑니다.

### ② 문서로 정리하기
토론이 충분히 됐으면 제안자가 `docs/01_ideas/_template.md`를 복사해 정리하고 PR을 올립니다.
문서 맨 아래 **참고 자료**에 Discussion 링크를 붙여 토론 기록을 이어줍니다.

### ③·④ 비교하고 결정하기
- 평가 기준은 이슈 #3에서 합의합니다.
- 결정한 내용은 Discussion에 댓글로 **→ 채택 / 보류 / 제외**를 남기고, 회의록에도 적습니다.

---

## 4. 데이터베이스처럼 찾아보기

### 라벨
| 라벨 | 뜻 |
|---|---|
| `idea` | 아이디어 |
| `research` | 리서치 |
| `design` | 디자인 |
| `android` | 앱 개발 |
| `pleos-sdk` | SDK 관련 |
| `docs` | 문서 |
| `meeting` | 회의 |
| `submission` | 제출물 |
| `bug` | 버그 |

이슈를 만들 때 **라벨을 꼭 하나 이상** 붙입니다. 그래야 나중에 찾을 수 있습니다.

### 자주 쓰는 검색 (Issues 검색창에 입력)
| 찾고 싶은 것 | 검색어 |
|---|---|
| 내가 맡은 일 | `is:open assignee:@me` |
| 아이디어 관련 이슈 | `label:idea` |
| 안드로이드 작업 중 진행 중인 것 | `is:open label:android` |
| 끝난 일 전체 | `is:closed` |
| 특정 단어 | `에뮬레이터 in:title,body` |

Discussions도 상단 검색창에서 같은 방식(`category:Ideas` 등)으로 검색할 수 있습니다.

### 폴더 = 자료실
| 폴더 | 들어가는 것 |
|---|---|
| `docs/00_contest-info/` | 공모전 요강·일정·심사기준, 제출 체크리스트 |
| `docs/01_ideas/` | 아이디어 문서, 비교표 |
| `docs/02_research/` | UX 리서치, 레퍼런스 |
| `docs/03_design/` | 화면 흐름, 와이어프레임 |
| `docs/04_roadmap/` | 주차별 로드맵 |
| `docs/05_submission/` | 발표자료, 시연영상 |
| `docs/progress/` | 주간 진행 보고 |
| `docs/meeting-notes/` | 회의록 |

---

## 5. 할 일 관리 (Issues + 보드)

1. **Issues → New issue** → 양식 선택 (💡 아이디어 제안 / ✅ 작업 / 🐞 버그)
2. 오른쪽에서 **Assignees**(담당자), **Labels**, **Projects: 공모전 준비**를 설정합니다.
3. 작업을 시작하면 보드에서 카드를 **In Progress**로 옮깁니다.
4. PR을 올리면 **Review**로 옮기고, 병합되면 **Done**으로 옮깁니다.
   (PR 본문에 `Closes #이슈번호`를 쓰면 병합할 때 이슈가 자동으로 닫힙니다.)

---

## 6. 소통 규칙

| 상황 | 규칙 |
|---|---|
| 카톡에서 결정된 것 | 관련 이슈나 Discussion에 **댓글로 요약**해서 남기기 |
| 질문 | 개인 DM보다 **Discussions Q&A**에 (다른 팀원도 같은 문제를 겪으니까) |
| 의견 요청 | `@아이디`로 콕 집어서 부르기 |
| 리뷰 요청이 오면 | **24시간 안에** 확인하기 |
| 회의 후 | 작성자가 `docs/meeting-notes/YYYY-MM-DD.md`에 회의록 작성, 액션 아이템은 이슈로 만들기 |
| 매주 일요일 | 각자 `docs/progress/`에 주간 보고 작성 |

---

## 7. 알림 설정 (놓치지 않기)

1. 저장소 오른쪽 위 **Watch** → **All Activity**를 고르면 모든 새 글·댓글 알림을 받습니다.
2. GitHub **모바일 앱**을 설치하면 휴대폰으로 알림이 옵니다.
3. 알림은 https://github.com/notifications 에서 모아 볼 수 있습니다.

---

## 8. 처음 참여하는 팀원 체크리스트

- [ ] GitHub 계정 만들고 아이디를 팀장에게 전달
- [ ] 초대 메일에서 **Accept invitation**
- [ ] 저장소 **Watch → All Activity** 설정
- [ ] GitHub 모바일 앱 설치
- [ ] [CONTRIBUTING.md](../CONTRIBUTING.md) 읽기 (브랜치·커밋·PR 규칙)
- [ ] 커밋 작성자 설정 (CONTRIBUTING.md 맨 위)
- [ ] Discussions에 자기소개 글 하나 쓰기 (General)
- [ ] 이슈 #4 개발 환경 세팅 완료 후 댓글 남기기

---

## 9. 자주 묻는 질문

**Q. 코드를 몰라도 참여할 수 있나요?**
A. 네. Discussions 글, 이슈 댓글, 보드 관리는 모두 웹에서 클릭만으로 됩니다. 문서도 GitHub 웹에서 파일을 열고 ✏️(연필) 버튼으로 수정한 뒤, **Create a new branch**를 선택하면 PR이 됩니다.

**Q. Discussion과 Issue가 헷갈려요.**
A. "누가 언제까지 해야 하는 일"이면 Issue, "이야기해 보고 싶은 것"이면 Discussion입니다. Discussion에서 할 일이 생기면 오른쪽의 **Create issue from discussion** 버튼으로 이슈를 만들 수 있습니다.

**Q. 잘못 올렸어요.**
A. 글과 댓글은 `···` 메뉴에서 수정할 수 있습니다. 삭제가 필요하면 팀장에게 말해 주세요.
