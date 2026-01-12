# Swole Scroll 📜💪

## 📖 The Story

I created Swole Scroll in November 2025 for a simple reason: my dad was texting himself his own max lifts to keep track of them, and I saw a better way.

I accepted the challenge to build a superior tool from scratch, treating my dad as my primary "client." The development cycle was intense and rapid—often implementing requested features or patching bugs overnight so the new build was ready for our gym session the next morning.

**The Technical Journey:**
* **The "Lightbulb Moment" with Room:** The steepest learning curve was wrapping my head around the Room Database abstraction and SQLite. I spent weeks studying the architecture, but once it clicked, the rest fell into place—allowing me to implement the full **JSON Import/Export system in under 10 minutes** later on.
* **The "Cardio Engine" Logic:** I chose to "reinvent the wheel" for cardio tracking (Floors/Time/Levels) rather than using standard libraries. This resulted in a unique, granular tracking experience that generic apps don't offer.

***

## 📸 Demo & Gallery

![swole-scroll-demo](https://github.com/user-attachments/assets/d980c090-1d63-4829-8259-a301dcb4a2ce)
*The app adapts UI inputs dynamically based on the exercise type (e.g., Distance/Weight for Carries vs. Reps/Weight for Press).*

### Feature Highlights

| **Long-Term Tracking** | **Session Journaling** |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/1e4a5cc4-1ac3-438e-be14-dc9d7b8e18cf" width="300" alt="History View"> | <img src="https://github.com/user-attachments/assets/cb032ee8-3915-42a8-a9a7-ce0a413129e4" width = "300" alt="Notes View" />
RE)" width="300" alt="Notes View"> |
| *Volume tracking with automatic PR calculation.* | *Contextual notes to track form cues and subjective feel.* |

| **Smart Architecture** | **Data Sovereignty** |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/4d6da64c-6170-40af-b4a8-8b107c222c4e" width="300" alt="Enum Dropdown"> | <img src="https://github.com/user-attachments/assets/2ea70a6c-46b9-4946-bcf2-b8be2d41c345" width="300" alt="Backup Menu"> |
| *Type-safe Enum selection triggers instant UI adaptation.* | *JSON Import/Export handling for complete data ownership.* |

***
