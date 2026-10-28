# app

> **아이템 확정 후 Android Studio 프로젝트를 이 폴더에 생성합니다.**

## 생성 방법 (아이템 확정 후)

1. Android Studio → **New Project**
2. 저장 위치(Save location)를 이 `app/` 폴더로 지정
3. 언어: **Kotlin**
4. Pleos SDK 의존성 추가 (공식 가이드 참고)
5. 차량용 에뮬레이터에서 실행 확인 후 `feature/project-setup` 브랜치로 PR

## 주의

- `local.properties`, `*.keystore`, `.env*`는 `.gitignore`로 제외되어 있습니다. 커밋하지 마세요.
