# 인스타폴리오(instafolio)
![Instafolio_Introduce](image/instafolio_introduce.png)

📷 많은 광고 업계 학생 및 취업 준비생들이   
포트폴리오 작업물로 PDF 파일을 인스타그램에 업로드하는 과정에서 불편함을 겪습니다.   
인스타폴리오는 **가장 쉽고 빠르게 인스타그램 포트폴리오를 만드는 앱**입니다.

<br>

## ✨ 핵심 기능

<p style="text-align: center;">
  <img src="image/instafolio_tooltip_2.png" width="30%"/>
  <img src="image/instafolio_tooltip_3.png" width="30%"/>
  <img src="image/instafolio_tooltip_4.png" width="30%"/>
</p>

<p style="text-align: center;">
  <img src="image/instafolio_tooltip_5.png" width="30%"/>
  <img src="image/instafolio_tooltip_1.png" width="30%"/>
</p>

- PDF / PNG / JPG 형식의 파일 저장 및 불러오기 지원
- 다수의 이미지를 인스타그램 규격(1:1)에 맞게 비율 간편 맞춤
- 한 페이지의 정보 밀도를 높일 수 있도록 두 장의 페이지를 한 장의 페이지로 묶어 병합
- Drag & Drop으로 페이지 순서 편집
- 최근 저장한 작업 재편집

<br>

## 👤 담당 역할

- Android 개발 전담
    - 앱 아키텍쳐 설계
    - 주요 기능 구현

<br>

## **🔧 주요 개발 내용**

### **MVVM Architecture**

- UI 로직과 상태 관리를 분리하기 위해 MVVM을 도입
- ViewModel에서 LiveData를 통해 UI 상태를 관리하고 View에서 구독하여 데이터 변경 시 UI가 자동으로 갱신되는 반응형 UI를 구축

### **다양한 파일 형식 입출력 지원**

- ContentResolver와 Intent 활용하여 파일의 Uri를 받아와서 활용
- PDF 파일인 경우 안드로이드 내장 PdfRenderer를 사용하여 각 페이지를 순회하며 이미지 Bitmap으로 변환
- 이미지를 PDF로 변환하려는 경우 itextpdf 라이브러리를 이용해 이미지 Bitmap을 PDF로 변환

### 로컬 데이터 관리

- 네트워크 연결 없이도 유저의 작업 내용을 안정적으로 보존하기 위해 Room 데이터베이스를 도입하고 Repository 패턴을 적용하여 데이터 계층을 설계
- 작업 결과물을 @Entity로 정의하고, 데이터의 삽입, 삭제, 조회를 위한 DAO 인터페이스를 설계

### **커스텀 Drag & Drop 기능**

- ItemTouchHelper.Callback을 상속하는 클래스를 구현하여 아이템 이동(onMove) 및 상태 변경(onItemSelected) 시의 동작을 직접 제어
- 두 아이템이 하나로 묶인 상태에서도 함께 이동하는 기능을 구현

<br>

## 🏗️ 아키텍쳐

![Architecture Diagram](image/instafolio_architecture_diagram.png)

UI 로직과 비즈니스 로직을 명확하게 분리하기 위해 MVVM 아키텍쳐를 적용함. 각 계층은 다음과 같은 책임을 가짐.

- **View**
    - 유저에게 보여지는 UI를 담당하며 모든 유저 이벤트를 ViewModel로 전달하는 역할을 수행함
- **ViewModel**
    - 화면에 필요한 데이터를 LiveData 형태로 보유하고 UI 상태(편집 모드 여부, 선택된 아이템 목록 등)을 관리
    - View로부터 이벤트를 전달받으면 Model 계층에 비즈니스 로직 수행을 요청함
- **Model**
    - 애플리케이션의 비즈니스 로직을 책임지는 계층
    - Repository는 데이터 입출력을 총괄하고 Room Database는 데이터를 기기 내에 영구적으로 저장함

<br>

## 🛠️ 기술 스택

| **Category** | **Tech Stack**                                                                                                                                                                                                                                                                                                                         |
| --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)                                                                                                                                                                                                                                  |
| **Platform** | ![Android](https://img.shields.io/badge/Native%20Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)                                                                                                                                                                                                                      |
| **Architecture** | ![MVVM](https://img.shields.io/badge/MVVM-4CAF50?style=for-the-badge)                                                                                                                                                                                                                                                                  |
| **Async** | ![Coroutine](https://img.shields.io/badge/Coroutine-0095D5?style=for-the-badge)                                                                                                                                                                                                                                                        |
| **Local Data** | ![Room](https://img.shields.io/badge/Room-4285F4?style=for-the-badge)                                                                                                                                                                                                                                                                  |
| **Jetpack** | ![LiveData](https://img.shields.io/badge/LiveData-00C853?style=for-the-badge), ![ViewModel](https://img.shields.io/badge/ViewModel-795548?style=for-the-badge), ![Navigation](https://img.shields.io/badge/Navigation-673AB7?style=for-the-badge), ![ViewBinding](https://img.shields.io/badge/ViewBinding-009688?style=for-the-badge) |

<br>

## **🧩 문제 해결 경험**

### **🔹리스트에서 아이템 복수 선택 UI 구현 문제**

**문제점**

- 연속된 두 개의 이미지 아이템을 하나로 묶는 기능이 기획됨
- 기본 RecyclerView 구조에서는 여러 아이템 위에 공통된 View를 그리는 것이 어려움

**해결 방법**

- 묶기 모드에서 한 아이템을 선택하면 바로 오른쪽 아이템도 함께 선택되도록 처리
- 하나의 공통 View를 생성하는 대신,
  각 아이템의 테두리에 left / right border를 개별적으로 그려
  시각적으로 하나의 박스처럼 보이도록 구현

**결과**

- RecyclerView를 사용하면서 복수 아이템 선택 UI 구현
- 유저가 자연스럽게 두 이미지를 하나의 단위로 인식할 수 있는 UX 제공

### **🔹연속된 두 아이템을 함께 Drag & Drop 할 수 없는 문제**

**문제점**

- 기존 RecyclerView를 이용하는 drag & drop 라이브러리들은 연속된 두 아이템 이동을 지원하지 않음

**해결 방법**

- ItemTouchHelper.Callback을 상속하는 ItemTouchHelperCallback 클래스를 구현하여
  아이템 이동(onMove) 및 상태 변경(onItemSelected) 시의 동작을 직접 제어
    - 유저가 묶인 아이템 중 하나를 드래그하면, 묶인 아이템 중 첫 번째 아이템을 타겟으로 설정
    - onItemSelected 콜백에서 같이 묶인 아이템을 찾아 Adapter의 데이터 리스트에서 일시적으로 제거
    - 제거된 아이템의 View는 드래그 중인 아이템의 ViewHolder에 시각적으로만 결합하여
      마치 두 아이템이 함께 움직이는 것처럼 보이도록 처리
    - 드래그가 끝나고 아이템을 놓는 시점(onItemClear)에 두 번째 아이템을 타겟 위치의 바로 오른쪽에 다시 삽입

**결과**

- 라이브러리가 지원하지 않는 '연속된 복수 아이템 이동' UX를 성공적으로 구현
- 기능 요구사항과 UX를 동시에 만족

<br>

## **🔄 개선 방향**

### 🔹 이미지 데이터로 인한 메모리 누수 및 OutOfMemory 위험

**문제**

- Fragment 간 이미지 데이터를 공유하기 위해 Shared ViewModel 사용
- Shared ViewModel은 Fragment보다 생명주기가 길어
  대용량 이미지 데이터가 메모리에 오래 남아있을 위험 존재

**개선 방향**

- Shared ViewModel 사용을 제거하고 Fragment별 ViewModel로 분리
- 화면 전환 전 이미지 데이터를 로컬에 임시 저장
- 전환 완료 후 필요한 시점에만 이미지 데이터를 다시 로드하도록 수정

**배운 점**

- ViewModel은 UI 상태 관리에 적합하며 대용량 데이터를 직접 보관하는 용도로 사용하면 안 됨
- 이미지와 같은 무거운 데이터는 로컬 파일 등 별도의 저장소에 두고 화면 전환 시에는 참조만 전달하는 구조가 바람직함

<br>

## 🔗 링크

- **Notion**  
  https://bouncy-rover-a1d.notion.site/2e2f88e182a580cf95e6c01261f14215?pvs=74
