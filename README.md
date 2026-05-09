# Gemini TTS Desktop

쓰려고 만든 매우 간단한 txt to tts 변환기
로컬에서 `.txt` 파일을 Gemini TTS API로 변환하는 데스크톱 앱
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