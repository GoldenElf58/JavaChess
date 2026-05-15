# Chess

## Setup Instructions

1. Download the JavaFX SDK from [here](https://gluonhq.com/products/javafx/)
2. Download IntelliJ IDEA
3. Clone this repository
4. Open the project in IntelliJ IDEA
5. Add the JavaFX SDK to the project
    1. Go to File > Project Structure > Libraries
    2. Click on the + button and select "Java"
    3. Navigate to the location of the JavaFX SDK and select the "lib" folder
    4. Click OK
    5. Go to Run > Edit Configurations
    6. Select Modify Options > Add VM Options
   7. Add the following arguments to the VM Options field, replacing '...' with the path to where
      the JavaFX SDK is installed
      1. `--module-path /<insert path here>/javafx-sdk-24.0.2/lib
--add-modules javafx.controls,javafx.fxml,javafx.web
--enable-native-access=javafx.graphics,javafx.media`
   8. Click OK
6. You're ready!

## Usage Instructions

### In Code Settings

Before starting the game, you can adjust the below settings within the code (all in `Main.java`,
starting around line 93)

- `runBenchmarkOnly` - Whether to run the benchmark or not (you can probably ignore this and 
  leave/set it to false if you're not benchmarking anything; also note that the benchmark could 
  be in different states based off of what has been benchmarked recently)
- `whitePlayerHuman` - Whether the white player is a human or a bot
- `blackPlayerHuman` - Whether the black player is a human or a bot
- `allottedTime` - the number of seconds the bot has to move

### UI Interactions

- Click or drag a piece to move it
- Use `cmd`+`z` / `left arrow` / `a` / `j` to undo a move
- Use `cmd`+`y` / `cmd`+`shift`+`z` / `right arrow` / `d` / `l` to redo a move
- Right click for pencil markings (drag for arrows)
- The text box lets you input an allotted time for the bot in seconds. It will set when you 
  press enter
- White/Black Human/CurrBot toggles between whether each side is bot or human
- Deep Test plays 1000 games between the two bots to see which one is better
