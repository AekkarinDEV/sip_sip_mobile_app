# SipSip - แอปติดตามการดื่มน้ำอัจฉริยะ & ปลูกต้นไม้จำลอง

SipSip คือแอปพลิเคชันบน Android ที่ช่วยให้การติดตามการดื่มน้ำในแต่ละวันเป็นเรื่องสนุกมากขึ้น ด้วยการนำระบบ **Gamification** มาผสมผสานกับการดูแลต้นไม้เสมือนจริง

ผู้ใช้สามารถตั้งเป้าหมายการดื่มน้ำ บันทึกปริมาณน้ำที่ดื่ม และปลูกต้นไม้ดิจิทัลที่เติบโตตามความสม่ำเสมอในการดูแลสุขภาพของตนเอง

---

## ✨ คุณสมบัติเด่น (Features)

### 💧 คำนวณเป้าหมายเฉพาะบุคคล
แอปจะคำนวณปริมาณน้ำที่ควรดื่มต่อวันโดยอัตโนมัติ โดยพิจารณาจาก

- น้ำหนักตัว
- เพศ
- ระดับการทำกิจกรรม

### 🥤 บันทึกการดื่มน้ำได้ง่าย
ผู้ใช้สามารถบันทึกการดื่มน้ำได้อย่างรวดเร็วผ่านปุ่มลัดของภาชนะขนาดต่าง ๆ เช่น

- ถ้วยกาแฟ
- ถ้วยชา
- แก้วน้ำขนาดเล็ก / ปกติ / ใหญ่

และยังสามารถระบุปริมาณน้ำด้วยตนเองได้

### 🌱 ระบบเกมปลูกต้นไม้ (Gamification)

SipSip เพิ่มความสนุกให้กับการดูแลสุขภาพผ่านระบบเกมเล็ก ๆ

- เมื่อดื่มน้ำครบเป้าหมายรายวัน ผู้ใช้จะได้รับ **ฝักบัวรดน้ำ**
- ฝักบัวสามารถใช้รดน้ำเพื่อพัฒนาการเติบโตของต้นไม้
- ผู้ใช้สามารถสะสม **Streak** จากการดื่มน้ำอย่างสม่ำเสมอในแต่ละวัน

### 📊 ประวัติและสถิติการดื่มน้ำ
สามารถดูข้อมูลย้อนหลังและวิเคราะห์พฤติกรรมการดื่มน้ำผ่านกราฟสถิติ

### 🔔 การแจ้งเตือน
ระบบแจ้งเตือนให้ผู้ใช้ดื่มน้ำเป็นระยะ โดยใช้

- WorkManager
- Firebase Cloud Messaging (FCM)

### 🎯 บทเรียนสอนการใช้งาน
มีระบบ **Interactive Tutorial (Coach Marks)** เพื่อช่วยแนะนำผู้ใช้ใหม่

### 🔐 ระบบสมาชิก
รองรับการสมัครสมาชิก ล็อกอิน และจัดการโปรไฟล์ผ่าน **Firebase Authentication**

---

## 🛠 เทคโนโลยีที่ใช้ (Technologies Used)

### ภาษา
- Kotlin

### UI Framework
- Android XML Layout
- Material Components
- ViewBinding

### Backend & Database
- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Messaging
- Firebase Storage

### Libraries
- Glide (Image loading)
- Lottie (Animations)
- MPAndroidChart (Statistics)
- TapTargetView (Tutorial)
- SweetAlert (UI Dialog)

### Background Tasks
- WorkManager

---

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
│   │   │   ├── res/                       # Layouts, Drawables, Themes
│   │   │   └── assets/                    # Lottie และข้อมูลต้นไม้
│   └── build.gradle.kts
└── build.gradle.kts
```

---

## 🚀 การติดตั้ง (Installation)

### 1. Clone repository

```bash
git clone https://github.com/yourusername/sip-sip-mobile-app.git
```

### 2. ตั้งค่า Firebase

1. ไปที่ **Firebase Console**
2. สร้างโปรเจกต์ใหม่
3. เพิ่ม Android app ด้วย package name

```
com.example.sip_sip_mobile_app
```

4. ดาวน์โหลดไฟล์ `google-services.json`
5. วางไฟล์ไว้ในโฟลเดอร์ `app/`

จากนั้นเปิดใช้งาน

- Email / Password Authentication
- Cloud Firestore
- Cloud Messaging

---

### 3. Build และ Run

1. เปิดโปรเจกต์ใน **Android Studio**
2. Sync Gradle
3. Run บน Emulator หรือ Android device

Minimum SDK: **29**

---

## 💡 วิธีใช้งาน (Usage)

1. กรอกข้อมูลส่วนตัวเพื่อให้ระบบคำนวณเป้าหมายการดื่มน้ำ
2. บันทึกการดื่มน้ำผ่านหน้าหลักของแอป
3. เมื่อดื่มน้ำครบเป้าหมาย จะได้รับฝักบัวสำหรับรดน้ำต้นไม้
4. ตรวจสอบสถิติการดื่มน้ำในหน้าสถิติ

---

## 📱 App Workflow

| สมัครสมาชิก | ล็อกอิน | ลืมรหัสผ่าน |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/5ecd7725-1d44-472c-a856-8c2b9cc9121d" width="220"/> | <img src="https://github.com/user-attachments/assets/09c94e61-33f7-4746-a6f1-fbfd34ced2a4" width="220"/> | <img src="https://github.com/user-attachments/assets/c5bf5803-8829-4a58-9285-dfa075c74879" width="220"/> |

| ตั้งค่าผู้ใช้ | เพิ่มรูปโปรไฟล์ | หน้าหลัก |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/41f5ed0d-213e-438a-a075-fb0d15536248" width="220"/> | <img src="https://github.com/user-attachments/assets/1fef7706-4c14-4833-b267-11f687235542" width="220"/> | <img src="https://github.com/user-attachments/assets/48c484ed-2ac2-4839-9349-0df2cfee4c31" width="220"/> |

| สถิติ | ระบบปลูกต้นไม้ | การตั้งค่า |
|---|---|---|
| <img src="https://github.com/user-attachments/assets/8545ac84-7849-44e0-a67b-63068b486545" width="220"/> | <img src="https://github.com/user-attachments/assets/ce4b8b75-864c-4eb5-9333-9642be4b2ca5" width="220"/> | <img src="https://github.com/user-attachments/assets/9e159fc0-5fb6-4676-b3dd-4988778e1042" width="220"/> |

---
