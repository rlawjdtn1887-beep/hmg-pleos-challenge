# 협업 규칙

## 브랜치 규칙

- `main`은 보호 브랜치입니다. **직접 push하지 않습니다.**
- 작업은 `main`에서 새 브랜치를 만들어 진행합니다.
  - 형식: `feature/작업명` (예: `feature/contest-info`, `feature/login-screen`)
  - 작업명은 영어 소문자와 하이픈(`-`)을 사용합니다.
- 작업이 끝나면 **Pull Request(PR)** 로 `main`에 병합합니다.

```bash
git checkout main
git pull
git checkout -b feature/작업명
# 작업 후
git push -u origin feature/작업명
```

## 커밋 메시지 규칙

```
<타입>: <요약>
```

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 추가·수정 (회의록, 리서치, 아이디어 등) |
| `refactor` | 동작 변화 없는 코드 구조 개선 |

예시:
- `docs: 공모전 요강 정리`
- `feat: 메인 화면 레이아웃 추가`
- `fix: 에뮬레이터에서 앱 종료되는 문제 수정`

## Pull Request 규칙

- PR 템플릿(변경 내용 / 확인 방법 / 스크린샷)을 채워주세요.
- **리뷰어 1명 이상의 승인(Approve)** 후 병합합니다.
- 관련 이슈가 있으면 본문에 `Closes #이슈번호`를 적어주세요.
- 병합 후 작업 브랜치는 삭제합니다.

## 이슈 & 보드

- 모든 작업은 이슈로 만들고 알맞은 라벨을 붙입니다.
  - `idea` `research` `design` `android` `pleos-sdk` `docs` `meeting` `submission` `bug`
- 이슈는 Projects 보드 "공모전 준비"에서 `Todo → In Progress → Review → Done` 순서로 옮깁니다.

## 주의사항

- `local.properties`, `*.keystore`, `.env*` 등 개인 설정·비밀 정보는 절대 커밋하지 않습니다.
- Pleos SDK 키 등 인증 정보는 팀 내부 채널로만 공유합니다.
