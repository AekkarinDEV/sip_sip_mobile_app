# SipSip - แอปติดตามการดื่มน้ำอัจฉริยะ & ปลูกต้นไม้จำลอง

SipSip คือแอปพลิเคชันบน Android ที่เปลี่ยนการติดตามการดื่มน้ำให้เป็นเรื่องสนุกด้วยระบบ Gamification ช่วยให้คุณรักษาความชุ่มชื้นของร่างกายในแต่ละวัน พร้อมเพลิดเพลินไปกับการดูแลสวนจำลอง เมื่อคุณดื่มน้ำครบตามเป้าหมาย คุณจะได้รับ "ฝักบัว" เพื่อนำไปรดน้ำและฟูมฟักต้นไม้ดิจิทัลของคุณให้เติบโต!

## คุณสมบัติเด่น (Features)

- **คำนวณเป้าหมายเฉพาะบุคคล**: คำนวณปริมาณน้ำที่ควรดื่มต่อวันโดยอัตโนมัติ ตามน้ำหนักตัว เพศ และระดับการทำกิจกรรม
- **บันทึกการดื่มน้ำได้ง่าย**: มีปุ่มลัดสำหรับภาชนะขนาดต่างๆ (ถ้วยกาแฟ, ถ้วยชา, แก้วน้ำขนาดเล็ก/ปกติ/ใหญ่) และสามารถระบุปริมาณเองได้
- **ระบบเกมปลูกต้นไม้ (Gamification)**: 
    - รับฝักบัวรดน้ำเมื่อดื่มน้ำครบตามเป้าหมายรายวัน
    - ใช้ฝักบัวเพื่อพัฒนาการเติบโตของต้นไม้ในระยะต่างๆ
    - สะสม "Streak" (ความต่อเนื่อง) จากการดูแลต้นไม้และดื่มน้ำทุกวัน
- **การแสดงผลที่สวยงาม**: ส่วนติดต่อผู้ใช้แบบ Water Drop View ที่เคลื่อนไหวตามเปอร์เซ็นต์ความก้าวหน้า
- **ประวัติการดื่มน้ำ**: ดูและจัดการรายการการดื่มน้ำย้อนหลังของวันได้ทันที
- **การแจ้งเตือน**: ระบบแจ้งเตือนให้ดื่มน้ำสม่ำเสมอผ่าน WorkManager และ Firebase Cloud Messaging (FCM)
- **บทเรียนสอนการใช้งาน**: มีระบบ Interactive Tutorial (Coach Marks) สำหรับแนะนำผู้ใช้ใหม่
- **ระบบสมาชิกที่ปลอดภัย**: สมัครสมาชิก ล็อกอิน และจัดการโปรไฟล์ผ่าน Firebase

## เทคโนโลยีที่ใช้ (Technologies Used)

- **ภาษา**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: Android XML Layouts พร้อม [Material Components](https://material.io/develop/android)
- **Backend & Database**: [Firebase](https://firebase.google.com/) (Auth, Firestore, Cloud Messaging, Storage)
- **การจัดการรูปภาพ**: [Glide](https://github.com/bumptech/glide)
- **แอนิเมชัน**: [Lottie](https://github.com/airbnb/lottie-android)
- **กราฟและสถิติ**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- **งานเบื้องหลัง (Background Tasks)**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **ระบบนำชมแอป**: [TapTargetView](https://github.com/KeepSafe/TapTargetView)
- **UI Utilities**: SweetAlert, ViewBinding

## 📂 โครงสร้างโปรเจกต์ (Project Structure)

```text
sip_sip_mobile_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sip_sip_mobile_app/
│   │   │   │   ├── MainActivity.kt        # หน้าหลักและระบบบันทึกการดื่มน้ำ
│   │   │   │   ├── Planting.kt            # ระบบเกมปลูกต้นไม้
│   │   │   │   ├── Statistics.kt          # หน้าวิเคราะห์สถิติ
│   │   │   │   ├── UserSetup.kt           # ตั้งค่าโปรไฟล์และคำนวณเป้าหมาย
│   │   │   │   ├── WaterDropView.kt       # UI ตัวหยดน้ำแบบ Custom
│   │   │   │   └── ...                    # ระบบยืนยันตัวตน, Worker, Service
│   │   │   ├── res/                       # ทรัพยากร UI (Layouts, Drawables, Themes)
│   │   │   └── assets/                    # ไฟล์ Lottie และข้อมูลต้นไม้
│   └── build.gradle.kts                   # การตั้งค่า Dependency ระดับ App
└── build.gradle.kts                       # การตั้งค่าโปรเจกต์ระดับ Root
```

## 🚀 การติดตั้ง (Installation)

1.  **Clone repository**:
    ```bash
    git clone https://github.com/yourusername/sip-sip-mobile-app.git
    ```
2.  **ตั้งค่า Firebase**:
    - สร้างโปรเจกต์ใหม่ใน [Firebase Console](https://console.firebase.google.com/)
    - เพิ่มแอป Android ด้วย package name `com.example.sip_sip_mobile_app`
    - ดาวน์โหลดไฟล์ `google-services.json` และนำไปวางในโฟลเดอร์ `app/`
    - เปิดใช้งาน **Email/Password Authentication** และ **Cloud Firestore**
3.  **Build & Run**:
    - เปิดโปรเจกต์ด้วย **Android Studio**
    - สั่ง Sync Gradle files
    - รันแอปบน Emulator หรืออุปกรณ์จริง (Minimum SDK: 29)

## 💡 วิธีใช้งาน (Usage)

1.  **เริ่มต้นใช้งาน**: กรอกข้อมูลส่วนตัว (น้ำหนัก, เพศ, กิจกรรม) เพื่อให้แอปคำนวณเป้าหมายรายวัน
2.  **การบันทึก**: กดที่ไอคอนแก้วน้ำบนหน้าหลักเพื่อบันทึกการดื่มน้ำ
3.  **การดูแลต้นไม้**: เมื่อดื่มครบเป้าหมาย คุณจะได้รับฝักบัว ให้ไปที่หน้า "Planting" เพื่อรดน้ำต้นไม้และดูการเติบโต
4.  **ดูสถิติ**: ตรวจสอบแนวโน้มการดื่มน้ำรายสัปดาห์หรือรายเดือนได้ที่หน้าสถิติ
## 📱 App Workflow Screenshots

<p align="center">

<b>1. สมัครสมาชิก</b><br>
<img src="https://github.com/user-attachments/assets/5ecd7725-1d44-472c-a856-8c2b9cc9121d" width="220">

<b>2. ล็อกอิน</b><br>
<img src="https://github.com/user-attachments/assets/09c94e61-33f7-4746-a6f1-fbfd34ced2a4" width="220">

<b>3. ลืมรหัสผ่าน</b><br>
<img src="https://github.com/user-attachments/assets/c5bf5803-8829-4a58-9285-dfa075c74879" width="220">

<br><br>

<b>4. ตั้งค่าข้อมูลผู้ใช้</b><br>
<img src="https://github.com/user-attachments/assets/41f5ed0d-213e-438a-a075-fb0d15536248" width="220">

<b>5. เพิ่มรูปโปรไฟล์</b><br>
<img src="https://github.com/user-attachments/assets/1fef7706-4c14-4833-b267-11f687235542" width="220">

<br><br>

<b>6. หน้าหลัก</b><br>
<img src="https://github.com/user-attachments/assets/48c484ed-2ac2-4839-9349-0df2cfee4c31" width="220">

<b>7. สถิติ</b><br>
<img src="https://github.com/user-attachments/assets/8545ac84-7849-44e0-a67b-63068b486545" width="220">

<b>8. ระบบปลูกต้นไม้</b><br>
<img src="https://github.com/user-attachments/assets/ce4b8b75-864c-4eb5-9333-9642be4b2ca5" width="220">

<br><br>

<b>9. การตั้งค่า</b><br>
<img src="https://github.com/user-attachments/assets/9e159fc0-5fb6-4676-b3dd-4988778e1042" width="220">

</p>

---
