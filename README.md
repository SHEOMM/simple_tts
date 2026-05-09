# Gemini TTS Desktop

로컬에서 `.txt` 파일을 Gemini TTS API로 변환하는 데스크톱 앱. Compose Multiplatform Desktop 기반, macOS / Windows 동작.

## 요구사항
- JDK 17 이상 (`java -version` 으로 확인)
- 인터넷 연결
- Gemini API 키: https://aistudio.google.com/apikey 에서 무료 발급

## 실행 (IntelliJ 불필요)

macOS / Linux:
```sh
./gradlew run
```

Windows:
```bat
gradlew.bat run
```

첫 실행 시 Gradle 8.13 distribution과 의존성을 자동 다운로드 (수 분 소요, 1회).

## 사용 흐름

1. 앱 실행 → API 키 입력 → "저장" (다음 실행 시 자동 로드)
2. "찾기..."로 입력 `.txt` 파일 선택. 출력 경로는 자동 채워짐 (`같은이름.wav`)
3. 모델 선택:
   - **Flash** — 무료 티어 가능. 기본 추천.
   - **Pro** — 더 자연스럽지만 결제 활성 필요 (https://console.cloud.google.com/billing)
4. 음성 선택 (한국어는 모든 음성에서 자동 인식되며, 음색만 차이남)
5. (선택) 스타일 지시 입력 — 어조/감정/속도 제어. 예:
   - `차분하고 부드러운 톤으로 천천히 낭독해주세요`
   - `감정을 살려 또박또박 읽어주세요`
6. "변환 시작" → 진행률·로그 확인 → "재생" 으로 즉시 재생

## CLI 모드 (배치 자동화용)

```sh
./gradlew run --args="sample.txt out.wav"
```
환경변수 `GEMINI_API_KEY` 또는 GUI에서 저장된 키를 사용. 모델/음성/스타일은 GUI에서 마지막 사용한 값 적용.

## 인스톨러 빌드 (선택)

```sh
./gradlew packageDistributionForCurrentOS
```
- macOS: `build/compose/binaries/main/dmg/GeminiTTS-1.0.0.dmg`
- Windows: `build/compose/binaries/main/msi/GeminiTTS-1.0.0.msi`

각 OS는 자기 자신만 패키징 가능 (cross-build 미지원). Windows 인스톨러를 만들려면 Windows에서 빌드.

## 버전 변경

모든 의존성 버전이 **`gradle/libs.versions.toml` 한 파일**에 모여 있음:
```toml
[versions]
kotlin = "2.1.20"
composeMultiplatform = "1.7.3"
kotlinxCoroutines = "1.9.0"
javaTarget = "17"
```

- Kotlin / Compose / 코루틴 버전: 위 파일 한 줄 수정
- Gradle 자체 버전: `gradle/wrapper/gradle-wrapper.properties` 의 `distributionUrl`
- 변경 후 `./gradlew build` 로 새 버전 적용

## API 키 저장 위치

`java.util.prefs.Preferences` 사용 (**평문**, OS 사용자 단위 저장):
- macOS: `~/Library/Preferences/dev.tts.gemini.plist`
- Windows: 레지스트리 `HKCU\Software\JavaSoft\Prefs\dev\tts\gemini`

앱 안 "지우기" 버튼으로 삭제 가능. 환경변수 `GEMINI_API_KEY`도 폴백으로 인정.

## 트러블슈팅

- **429 Quota exceeded, model: gemini-2.5-pro-tts** — Pro 모델은 결제 활성이 필요합니다. Flash로 바꾸거나 결제를 활성화하세요.
- **403 / 400 API key invalid** — 새 키를 발급해 다시 저장하세요.
- **출력 음성이 첫 청크에서 끊김** — 분당 호출 한도(특히 Flash)에 걸렸을 가능성. 잠시 후 재시도하거나 짧은 입력으로 테스트.
