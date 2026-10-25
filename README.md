# JavaChess

A desktop chess game built with Java and JavaFX.

## Play a release build

Download the archive for your operating system from the [GitHub Releases page](https://github.com/GoldenElf58/JavaChess/releases) and unzip it. The release includes its own Java runtime, so you do not need to install Java or JavaFX.

- **Windows:** launch `JavaChess.exe` from the extracted folder.
- **macOS:** move `JavaChess.app` to Applications and open it. Since the release is not signed yet, macOS may require Control-clicking the app and choosing **Open** the first time.
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
