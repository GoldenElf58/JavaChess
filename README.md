# JavaChess

A desktop chess game built with Java and JavaFX.

## Play a release build

Download the archive for your operating system from the [GitHub Releases page](https://github.com/GoldenElf58/JavaChess/releases) and unzip it.

- **Windows:** launch `JavaChess.exe` from the extracted folder.
- **macOS:** move `JavaChess.app` to Applications and open it. If you get a message that says "Apple could not verify “tractor-ai-macos” is free of malware that may harm your Mac or compromise your privacy," click " Done". Go to System Settings/Privacy & Security and scroll down to the Security section. Click "Open Anyway" next to the message that says "tractor-ai-macos was blocked to protect your app". Click " Open Anyway" one more time, then type in your password. You're good to go!
- **Linux:** run `JavaChess/bin/JavaChess` from the extracted folder.

## Run from source

Install a JDK 25 or newer, then run:

```sh
./gradlew run
```

On Windows, use `gradlew.bat run` in Command Prompt or PowerShell. Gradle downloads the required JavaFX libraries the first time it runs.

To build a runnable app image for your current operating system:

```sh
./gradlew jpackageImage
```

The app image is created under `build/jpackage/`. Build it on the same operating system you plan to run it on.

## Publish a GitHub release

Push a version tag to GitHub to build release downloads for Windows, macOS, and Linux:

```sh
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions packages the game and attaches the archives to a draft-free GitHub Release. Open the repository's **Releases** page to see the build status and downloads. Use a new version tag, such as `v1.0.1`, for each release.

## Controls

- Click or drag a piece to move it.
- Undo with `Cmd+Z`, the left arrow, `A`, or `J`.
- Redo with `Cmd+Y`, `Cmd+Shift+Z`, the right arrow, `D`, or `L`.
- Right-click and drag to draw arrows on the board.
- Change the bot's move time in the text box and press Enter.
- Use the White/Black Human/Bot buttons to choose who plays each side.
