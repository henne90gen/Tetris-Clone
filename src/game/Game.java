package game;

import java.awt.Canvas;
import java.awt.image.DataBufferInt;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import game.graphics.GameOverScreen;
import game.graphics.ScoreCounter;
import game.graphics.Vector2D;
import game.graphics.Window;

public class Game implements Runnable {

	// General Settings
	public static final int WIDTH = TileMap.TILE_SIZE * TileMap.TILE_MAP_WIDTH;
	public static final int HEIGHT = TileMap.TILE_SIZE * TileMap.TILE_MAP_HEIGHT;
	public static final String NAME = "Tetris";
	public static final int SPEED = 3;
	public static final int TURN_TIME = 200;
	public static final int DROP_DELAY = 50;

	public static Window window;
	public static boolean running = false;
	public static boolean gameOver = false;
	public static int tickCount = 0;
	public int lastTickCount = 0;
	public int lastTickCountInput = 0;
	public double lastTurn = System.currentTimeMillis();
	public int[] pixels = null;
	public ScoreCounter scoreCounter;
	public GameOverScreen gos;

	// Variables for input handling
	public static final String PRESSED = "pressed";
	public static final String RELEASED = "released";
	public static Map<Direction, Boolean> keyMap = new EnumMap<Direction, Boolean>(Direction.class);

	/**
	 * Initializes all important variables - main window - shapes - pixels -
	 * input
	 */
	public Game() {

		window = new Window(NAME, WIDTH, HEIGHT);
		
		scoreCounter = new ScoreCounter(new Vector2D(TileMap.TILE_MAP_WIDTH - 4, 0));

		gos = new GameOverScreen(new Vector2D(TileMap.TILE_MAP_WIDTH / 2, TileMap.TILE_MAP_HEIGHT / 2), scoreCounter);

		TileMap.init(scoreCounter);

		running = true;

		pixels = ((DataBufferInt) window.getBufferedImage().getRaster().getDataBuffer()).getData();

		setKeyBindings();
	}

	/**
	 * Handles all the input that was added to the keyMap
	 */
	private void input() {
		for (Direction dir : Direction.values()) {
			if (keyMap.get(dir)) {
				if (dir == Direction.ESCAPE) {
					System.exit(0);
				} else if (dir == Direction.TURN && System.currentTimeMillis() - lastTurn > TURN_TIME) {
					TileMap.turn();
					lastTurn = System.currentTimeMillis();
				} else if (dir != Direction.UP) {
					if (lastTickCountInput + DROP_DELAY - 40 < tickCount) {
						TileMap.move(dir);
						lastTickCountInput = tickCount;
					}
				}
			}
		}
	}

	/**
	 * Updates the game-logic
	 */
	public void update() {
		tickCount++;
		if (lastTickCount + DROP_DELAY < tickCount) {
			TileMap.move(Direction.DOWN);
			lastTickCount = tickCount;
		}
		window.getCanvas().requestFocusInWindow();
	}

	/**
	 * Renders Background, TileMap and ScoreCounter
	 */
	public void render() {
		if (!gameOver) {
			// Paint the background
			for (int i = 0; i < pixels.length; i++) {
				int color = (110 << 16) + (110 << 8) + 0;
				pixels[i] = color;
			}

			// Paint the borders and the game tiles on the background
			pixels = TileMap.addToPixels(pixels);

			// Paint the current score
			pixels = scoreCounter.addToPixels(pixels);

		} else {
			pixels = gos.addToPixels(pixels);
		}
		window.render();
	}

	@Override
	public void run() {
		long last = System.nanoTime();
		long timer = System.nanoTime();
		double nsPerTick = 1000000000D / 60D;
		double delta = 0D;
		int fps = 0;
		int ups = 0;
		
		while (running) {
			long now = System.nanoTime();
			delta += (now - last) / nsPerTick;
			last = now;

			while (delta >= 1) {
				input();
				update();
				ups++;
				delta -= 1;
			}
			render();
			fps++;
			
			if (System.nanoTime() - timer > 1000000000){
				timer += 1000000000;
				window.setTitle(NAME + " ups: " + ups + ", fps: " + fps);
				fps = 0;
				ups= 0;
			}
			
			if (!window.isDisplayable())
				running = false;
		}
	}

	private void setKeyBindings() {
		for (Direction dir : Direction.values()) {
			keyMap.put(dir, Boolean.FALSE);
		}
		InputMap inputMap = window.getInputMap();
		ActionMap actionMap = window.getActionMap();
		for (Direction dir : Direction.values()) {
			KeyStroke keyPressed = KeyStroke.getKeyStroke(dir.getKeyCode(), 0, false);
			KeyStroke keyReleased = KeyStroke.getKeyStroke(dir.getKeyCode(), 0, true);

			inputMap.put(keyPressed, dir.toString() + PRESSED);
			inputMap.put(keyReleased, dir.toString() + RELEASED);

			actionMap.put(dir.toString() + PRESSED, new InputHandler(dir, PRESSED));
			actionMap.put(dir.toString() + RELEASED, new InputHandler(dir, RELEASED));
		}
	}

	public static int getWindowWidth() {
		return window.getWidth();
	}

	public static int getWindowHeight() {
		return window.getHeight();
	}

	public static Canvas getCanvas() {
		return window.getCanvas();
	}

	public static void stop() {
		running = false;
	}

	public static void gameOver() {
		gameOver = true;
	}

	/**
	 * Shows a custom error popup on the current window
	 * 
	 * @param window
	 * @param error
	 */
	public static void printErrorMessage(Window window, String error) {
		JOptionPane.showMessageDialog(window, error);
	}

	public static void main(String[] args) {
		new Thread(new Game()).start();
	}
}
