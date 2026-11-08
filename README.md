# Jaguar Block: Navigate the Unknown

발판의 색과 경로를 관측하고 기억하여 진행하는 LibGDX 2D 플랫포머입니다.
핵심 기믹은 데스 마커, 안전색 전환, 주기적인 좌우 입력 반전입니다.

## 실행

Android Studio에서 이 README와 `settings.gradle`이 있는 **JaguarBlock 폴더**를 엽니다.
Gradle JDK는 Android Studio의 내장 JBR 21을 선택합니다.
동기화가 끝나면 Gradle의 `lwjgl3 > Tasks > application > run`을 실행합니다.
게임은 LWJGL3의 별도 PC 창으로 열립니다.

Windows에서는 `run-game.bat`로도 실행할 수 있습니다. 이 파일은 기본 설치 경로의
Android Studio JDK를 찾습니다. 다른 위치의 JDK는 `JAVA_HOME`으로 지정합니다.

JDK가 설정된 터미널에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat :lwjgl3:run
```

Gradle과 의존성이 없는 PC에서는 첫 실행 때 인터넷 연결이 필요합니다.

## 조작

| 키 | 동작 |
| --- | --- |
| A/D 또는 좌우 방향키 | 이동 |
| Space | 점프 |
| F | 섬광탄 |
| R | 현재 스테이지 처음부터 다시 시작 |
| Esc | 일시정지 및 재개 |
| M | 음악 음소거 및 복구 |

관측판을 밟고 있는 동안 다음 구간의 색을 확인할 수 있습니다.
현재 안전색과 같은 발판을 밟고, 같은 색의 깃발에서 저장합니다.
위험 발판에서 사망하면 묘비가 남아 그 발판의 원래 색을 보여줍니다.
낙사하면 목숨 차감 없이 마지막으로 안전하게 밟았던 위치로 돌아갑니다.
세 스테이지를 모두 완료하면 전체 클리어가 표시됩니다.

## 프로젝트 구조

```text
assets/       실제 게임에서 사용하는 이미지, 음악, 폰트와 폰트 라이선스
core/         게임 로직, 캐릭터, 발판, 카메라와 화면 그리기
lwjgl3/       PC 실행 진입점과 실행 모듈 설정
gradle/       Gradle Wrapper
.run/         Android Studio 공유 실행 설정
```

소스 대상은 Java 8이며 Gradle Wrapper 8.14.4, LibGDX 1.14.2를 사용합니다.
나눔고딕의 라이선스는 `assets/fonts/OFL.txt`에 포함되어 있습니다.

