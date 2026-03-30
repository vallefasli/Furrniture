## Technical Overview

**Furr-niture** is a modern Android application built with **Jetpack Compose** that allows users to "rescue" cats by documenting them through photography and integrating them into a virtual home environment. The application follows a reactive architecture and leverages multiple AI services for image analysis and creative content generation.

### Core Architecture
* **UI Framework**: Developed entirely using **Jetpack Compose**, featuring a single-activity architecture with a custom state-driven navigation system managed via the `StoryKittyApp` composable.
* **State Management**: Utilizes a **ViewModel** (`PixelCatViewModel`) to handle business logic, asynchronous API calls, and UI state.
* **Data Persistence**: Employs the **Room Database** library to store persistent records for each "rescued" resident, including their name, breed, image paths, and spatial coordinates.
* **Concurrency**: Uses **Kotlin Coroutines** and **Flow** for non-blocking database operations and networking.

### Key Features
* **AI-Powered Identification**: Integrates the **Gemini 1.5 Flash** model to verify if a photograph contains a feline and to identify its specific breed.
* **Sticker Generation Modes**: Offers four distinct "Rescue Modes" for processing feline images:
    * **Simple**: Saves the original photo for the scrapbook without additional processing.
    * **Breed Only**: Removes the background from the original photo to create a sticker.
    * **Pollination**: Uses the **Pollinations.ai API** to generate high-quality 8-bit pixel art stickers based on the identified breed.
    * **Pixel Lab**: Utilizes the **PixelLab API** to generate specialized 8-bit vector-style stickers using a reference image.
* **Image Processing**: Features automated background removal via the **Remove.bg API** to ensure generated stickers blend seamlessly into virtual rooms.
* **Virtual Room System**: Includes a spatial management system where users can place stickers in different rooms and persist their exact coordinates (`posX`, `posY`) to the database.
* **Dynamic Scrapbook**: Provides a "Meow-ments" collection screen that renders cat profiles as randomized, polaroid-style cards with dynamic sticker overlays.

### Tech Stack
* **Language**: Kotlin
* **UI**: Jetpack Compose (Material 3)
* **Networking**: OkHttp3 for API communication and Coil for image loading
* **AI Models**: Google Generative AI (Gemini), Pollinations.ai, and PixelLab
* **Storage**: Room Persistence Library
* **Minimum SDK**: API 26 (Android 8.0)
