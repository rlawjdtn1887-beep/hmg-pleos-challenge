# 협업 규칙

## 처음 한 번: 커밋 작성자 설정

커밋이 **자기 GitHub 계정의 기여로 기록되도록** 저장소를 clone한 뒤 한 번 설정합니다.
이메일은 GitHub → Settings → Emails의 `...@users.noreply.github.com` 주소를 쓰면 개인 이메일이 공개되지 않습니다.

```bash
git config user.name "내GitHub아이디"
git config user.email "숫자+내GitHub아이디@users.noreply.github.com"
```

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

## 진행 보고

- 매주 각자 [docs/progress/](docs/progress/)에 진행 보고를 작성합니다.

## APK 배포 (Releases)

- APK 파일은 저장소에 커밋하지 않고 **Releases**에 첨부합니다.
- 버전 규칙: `v<주>.<부>.<수정>` (예: `v0.1.0` 첫 시연용, `v1.0.0` 최종 제출본)
- 순서: 저장소 → **Releases** → **Draft a new release** → 태그(`v0.1.0`) 생성 → 변경 내용 작성 → APK 첨부 → **Publish**
- 배포 후 README의 "다운로드" 표를 업데이트합니다.

## 주의사항

- `local.properties`, `*.keystore`, `.env*` 등 개인 설정·비밀 정보는 절대 커밋하지 않습니다.
- APK 서명용 keystore 파일과 비밀번호는 저장소·Releases 어디에도 올리지 않습니다.
- Pleos SDK 키 등 인증 정보는 팀 내부 채널로만 공유합니다.
