# 릴리스 빌드 R8 규칙. 라이브러리 규칙은 각 라이브러리가 함께 넣어 주므로 여기는 앱에서 필요한 것만.

# 클래스를 한 패키지로 모아 구조를 숨기고 크기를 줄인다
-repackageclasses ''
-allowaccessmodification

# 크래시 기록의 줄 번호는 남기고 원래 파일 이름은 숨긴다 (mapping.txt로 되돌림)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 릴리스에서 디버그 로그 제거 (경고·오류 로그는 남긴다)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Room(WorkManager가 Glance 위젯 갱신에 씀)은 DB 구현 클래스를 이름으로 찾는다
-keep class * extends androidx.room.RoomDatabase { <init>(); }
