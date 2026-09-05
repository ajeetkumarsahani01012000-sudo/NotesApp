# NoteApp — Stylus Note-Taking App

Ye ek working Android Studio project hai. Isme ye features hain:

- Stylus/finger drawing canvas (pressure-sensitive strokes)
- 3 tools: Pen, Highlighter, Eraser
- 6 colors + adjustable stroke width
- Undo / Redo
- Stylus-only mode (palm rejection — jab on ho, finger touch ignore hoga)
- Multiple notes, auto-saved locally (Room database)
- Notes list screen (tap to open, long-press to delete)
- Rename note (toolbar title pe tap karo)
- **PDF Import**: Kisi bhi PDF ko note mein le aao — har PDF page ek annotatable page ban jata hai, jispe stylus se likh/highlight/erase kar sakte ho
- **PDF Export**: Poora note (sare pages, PDF background + handwriting) ek single PDF file mein export hota hai — Downloads folder mein save hoti hai
- **Multi-page notes**: Prev/Next se pages ke beech move karo, "+ Page" se blank page add karo, "🗑 Page" se current page delete karo

## Kaise open karein

1. **Android Studio** install karo (agar nahi hai): https://developer.android.com/studio
2. Android Studio kholo → **Open** → is `NoteApp` folder ko select karo
3. Pehli baar Gradle sync hoga (internet chahiye, dependencies download hongi) — thoda time lagega
4. Agar Android Studio "Gradle wrapper missing" bole, to **File → Sync Project with Gradle Files** dabao, ya jab prompt aaye "Create Gradle Wrapper" accept kar lo
5. Ek emulator banao (**Device Manager**) ya apna phone USB se connect karo (USB debugging on karke)
6. Green **Run ▶** button dabao

## Project structure

```
app/src/main/java/com/example/noteapp/
├── data/
│   ├── Stroke.kt          ← ek stroke ka data (points, color, pressure)
│   ├── NoteEntity.kt      ← note ka basic info (title, dates)
│   ├── PageEntity.kt      ← har page ka data (strokes + PDF background path)
│   ├── NoteDao.kt / PageDao.kt ← database queries
│   ├── NoteDatabase.kt    ← Room database setup
│   └── NoteRepository.kt  ← save/load + PDF import/export logic (yahi sabse important file hai)
├── ui/
│   ├── DrawingView.kt     ← canvas jaha drawing hoti hai (background image bhi dikhata hai)
│   ├── StrokeRenderer.kt  ← shared drawing logic (screen aur PDF export dono use karte hain)
│   ├── MainActivity.kt    ← notes list screen
│   └── NoteEditorActivity.kt ← drawing screen + pages + PDF import/export ka logic
└── adapter/
    └── NotesAdapter.kt    ← notes list ka RecyclerView adapter
```

## PDF import/export kaise kaam karta hai

- **Import**: `PdfRenderer` (Android ka built-in API) se PDF ke har page ko image mein render karke usko ek naye page ka background bana dete hain. Phir uspe normal drawing hoti hai.
- **Export**: `PdfDocument` API se har page ka background image + upar ki handwriting ek PDF page mein draw karke, sabko combine karke ek `.pdf` file Downloads folder mein save ho jati hai.
- Dono ke liye koi extra library nahi chahiye — pure Android SDK use hua hai.

## Aage kya improve kar sakte ho

- **Zoom/Pan**: `DrawingView` mein `ScaleGestureDetector` add karke infinite canvas
- **Shapes tool**: seedhi line/circle auto-correct karne ke liye
- **Text notes**: drawing ke saath text boxes bhi add karna
- **Handwriting → Text**: ML Kit Digital Ink Recognition API use karke
- **Cloud sync**: Firebase ya apna backend add karke
- **Page reorder / thumbnails**: page navigation ko grid-view mein dikhana

Sabse pehle app ko run karke dekho, phir jo feature chahiye bolo — hum us par kaam karenge.
