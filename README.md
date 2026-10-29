# Phone Monitor (폰 모니터)

휴대폰의 배터리·메모리·저장 공간·CPU 속도·발열·네트워크 속도를 보여 주는 앱과 홈 화면 위젯.

- Android 10(API 29) 이상. 그 아래 기기에는 Play 스토어가 설치 버튼을 보여 주지 않는다
- Kotlin, Jetpack Compose(앱 화면), Jetpack Glance(위젯)
- 문구: 영어(기본), 한국어(`values-ko`)

## 구조

```
data/       StatsReader (기기 상태 읽기), StatsStore (앱·위젯·알림이 함께 보는 마지막 값), Labels (단위·문구)
ui/         DashboardScreen, SettingsScreen, PermissionScreen (첫 실행 권한 안내), Components (게이지·애니메이션), Theme
permission/ AppPermission (요청하는 권한 목록, 필요할 때 바로 요청 / 막혔으면 설정 화면으로)
widget/     Widgets (위젯 5종 등록·크기 구분), WidgetParts (공통 부품·링 그림), *Style.kt (모양별 화면)
cleanup/    Cleanup (제조사별 정리 화면으로 이동: 삼성 디바이스 케어, 샤오미, 화웨이, 오포, 비보 → 없으면 저장 공간 정리)
live/       LiveUpdateService (실시간 모드: 포그라운드 서비스로 3초마다 위젯·알림 갱신), BootReceiver (재부팅 뒤 복원)
src/debug/  WidgetGalleryActivity (개발용: 위젯 5종 × 크기를 한 화면에, 미리보기 PNG 만들기)
```

## 동작

- 첫 실행: 스플래시 → 권한 안내(없는 권한만 하나씩, 거절해도 다음으로) → 대시보드
- 권한은 필요한 순간에 다시 묻는다. 두 번 거절해 시스템이 막으면 앱 설정 화면을 연다. 설정 화면에서 상태 확인·요청

- 앱 화면: 보이는 동안 1초마다 갱신
- 위젯: 기본 30분마다 + ↻ 버튼. 앱에서 "위젯 실시간 갱신"을 켜면 화면이 켜져 있는 동안 3초마다 (알림 표시)
- 실시간 모드는 켠 상태를 저장한다. 재부팅·앱 업데이트 뒤(`BootReceiver`), 강제 종료 뒤 앱을 열 때 다시 켠다. 알림의 [끄기]나 스위치로 끄면 꺼 둔다
- 부팅 신호는 여러 앱에 차례로 전달되므로 재부팅 뒤 켜지기까지 1분쯤 걸릴 수 있다

## Android가 막아 둔 것

- **메모리 정리:** Android 14부터 앱은 다른 앱을 종료할 수 없다 → [정리하기]는 휴대폰의 정리 화면을 연다. 제조사 화면은 버전마다 위치가 달라 차례로 시도하고, 실패하면 저장 공간 정리 화면. 새 제조사를 넣을 때는 `AndroidManifest.xml`의 `<queries>`에도 추가

- **CPU 사용률:** Android 8부터 일반 앱은 읽을 수 없다 → 코어별 현재 속도를 보여 준다
- **칩 온도:** 공개 API 없음 → 배터리 온도 + 발열 단계(`PowerManager.currentThermalStatus`)와 성능 제한까지 남은 여유(`getThermalHeadroom`, Android 11+)
- **충전 전류:** 기기마다 단위·부호가 달라 참고용
- 에뮬레이터는 CPU 속도를 가짜 값(1~2kHz)으로 줘서 "공개하지 않음"으로 나온다. 실제 기기에서 확인

## 위젯

모양 5종(클래식, 링, 그라데이션, 터미널, 큰 숫자), 각각 크기별 배치 5가지(1×1, 2×1, 2×2, 4×1, 4×2)와 높은 기기용(190dp 이상) 추가 항목.
2칸 높이는 기기마다 140~200dp라서 2×2·4×2 배치는 140dp에 들어가게 만든다.

```bash
# 전부 그려 보기 (style 생략 시 5종 모두)
adb shell am start -n com.yunseong.phonemonitor/.WidgetGalleryActivity --es style ring
# 위젯 미리보기 PNG + 스토어 아이콘(512) 다시 만들기
adb shell am start -n com.yunseong.phonemonitor/.WidgetGalleryActivity --ez export true
for s in classic ring gradient terminal focus; do adb pull /sdcard/Android/data/com.yunseong.phonemonitor/files/previews/preview_$s.png app/src/main/res/drawable-nodpi/; done
adb pull /sdcard/Android/data/com.yunseong.phonemonitor/files/previews/ic_launcher_512.png store/
# 다른 제조사 정리 화면 시도 (해당 기기가 아니면 저장 공간 화면으로 넘어가는지)
adb shell am start -n com.yunseong.phonemonitor/.WidgetGalleryActivity --es cleanup samsung
```

## Glance 주의

- Column/Row는 자식을 10개까지만 그린다. 넘으면 조용히 잘리므로 묶어서 쓴다
- 위젯이 떠 있는 동안에는 `provideGlance`가 다시 불리지 않는다. 새 값은 `StatsStore`(StateFlow)로 전달
- 호(링)는 그릴 수 없어 비트맵으로 넣는다 (`ringBitmap`). 셰이더를 써도 Paint 색의 투명도가 곱해지므로 불투명으로 둔다

## 스토어 출시 전

- [ ] `applicationId`(`com.yunseong.phonemonitor`) 확정. 첫 업로드 뒤에는 못 바꾼다
- [ ] 서명 키 만들기 (keystore는 git에 올리지 않음, `.gitignore`에 있음)
- [ ] Play Console "포그라운드 서비스 권한" 신고: `specialUse` 사유 (AndroidManifest의 설명과 같게) + 동작 영상
- [ ] 개인정보처리방침 URL (수집·전송하는 데이터 없음으로 작성)
- [ ] 스토어 스크린샷 (위젯 미리보기 PNG, 스토어 아이콘 `store/ic_launcher_512.png`는 있음)
- [ ] 삼성·샤오미 등 실제 기기에서 [정리하기]가 제조사 화면을 여는지 확인
- [ ] 실제 기기 여러 대에서 CPU 속도·전류 값 확인

## 릴리스 빌드 (코드 축소·난독화)

- R8로 안 쓰는 코드·리소스 제거, 클래스 이름 난독화·한 패키지로 재배치, 릴리스에서 `Log.v/d/i` 제거 (`app/proguard-rules.pro`)
- 출시할 때마다 `app/build/outputs/mapping/release/mapping.txt`를 보관한다. 난독화된 크래시 기록을 원래 이름으로 되돌릴 때 필요 (AAB로 올리면 Play Console이 함께 받음)
- 릴리스 빌드는 디버그와 다르게 동작할 수 있으니 출시 전 꼭 실제로 설치해 본다. 예: Glance가 끌어오는 WorkManager 2.7.1은 난독화하면 시작 때 죽어서 2.12.0으로 고정함

## 빌드

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew installDebug         # 연결된 기기에 설치
./gradlew testDebugUnitTest    # 단위 테스트
```
