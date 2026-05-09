# Gemini TTS Desktop

쓰려고 만든 매우 간단한 txt to tts 변환기

## 요구사항
- JDK 17 이상 (`java -version` 으로 확인)
- 인터넷 연결
- Gemini API 키: https://aistudio.google.com/apikey 에서 발급

## 실행

macOS / Linux:
```sh
./gradlew run
```

Windows:
```bat
gradlew.bat run
```

## CLI 모드 (배치 자동화용)

```sh
./gradlew run --args="sample.txt out.wav"
```
환경변수 `GEMINI_API_KEY` 또는 GUI에서 저장된 키를 사용.

## 인스톨러 빌드 (선택)

```sh
./gradlew packageDistributionForCurrentOS
```
- macOS: `build/compose/binaries/main/dmg/GeminiTTS-1.0.0.dmg`
- Windows: `build/compose/binaries/main/msi/GeminiTTS-1.0.0.msi`

## 지원 모델
- `gemini-3.1-flash-tts-preview` (기본) — 표현 태그 지원
- `gemini-2.5-flash-preview-tts` — 무료 티어
- `gemini-2.5-pro-preview-tts` — 결제 활성 필요

3.1은 텍스트에 인라인 태그를 넣어 어조 제어 가능. 예:
```
[whispers] 비밀이야. [excited] 진짜 신나! [sad] 아쉬워...
```
사용 가능 태그: `[whispers]`, `[shouting]`, `[excited]`, `[bored]`, `[amazed]`, `[crying]`, `[curious]`, `[sighs]`, `[gasp]`, `[giggles]`, `[laughs]`, `[very slow]` 등.