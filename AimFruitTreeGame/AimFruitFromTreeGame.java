import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class AimFruitFromTreeGame extends JPanel implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
    // Game constants
    private static final int GAME_WIDTH = 1000;
    private static final int GAME_HEIGHT = 700;
    private static final int CANNON_WIDTH = 80;
    private static final int CANNON_HEIGHT = 60;
    private static final int FRUIT_SIZE = 25;
    private static final int BULLET_SIZE = 8;
    private static final int TREE_WIDTH = 120;
    private static final int TREE_HEIGHT = 200;
    
    // Game variables
    private int cannonX = 50;
    private int cannonY = GAME_HEIGHT - 150;
    private double cannonAngle = 0; // Angle in radians
    private int mouseX, mouseY;
    private int score = 0;
    private int level = 1;
    private int fruitsRemaining = 0;
    private boolean gameRunning = true;
    private int ammo = 20;
    
    // Game objects
    private ArrayList<Tree> trees;
    private ArrayList<Bullet> bullets;
    private ArrayList<FallingFruit> fallingFruits;
    private Timer gameTimer;
    private Random random;
    private long lastShot = 0;
    
    // Wind effect
    private double windForce = 0;
    private long windChangeTime = 0;
    
    public AimFruitFromTreeGame() {
        this.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        this.setBackground(new Color(135, 206, 235)); // Sky blue
        this.setFocusable(true);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        
        trees = new ArrayList<>();
        bullets = new ArrayList<>();
        fallingFruits = new ArrayList<>();
        random = new Random();
        
        initializeLevel();
        
        // Start game timer (60 FPS)
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }
    
    private void initializeLevel() {
        trees.clear();
        bullets.clear();
        fallingFruits.clear();
        ammo = 15 + (level * 5);
        fruitsRemaining = 0;
        
        // Create trees with fruits
        int numTrees = 2 + level;
        for (int i = 0; i < numTrees; i++) {
            int treeX = 300 + (i * 200) + random.nextInt(50);
            int treeY = GAME_HEIGHT - 250 - random.nextInt(50);
            Tree tree = new Tree(treeX, treeY);
            
            // Add fruits to tree
            int fruitsPerTree = 3 + random.nextInt(4);
            for (int j = 0; j < fruitsPerTree; j++) {
                int fruitX = treeX + 20 + random.nextInt(TREE_WIDTH - 40);
                int fruitY = treeY + 30 + random.nextInt(80);
                tree.addFruit(new TreeFruit(fruitX, fruitY));
                fruitsRemaining++;
            }
            trees.add(tree);
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw(g2d);
    }
    
    public void draw(Graphics2D g) {
        if (gameRunning) {
            // Draw ground
            g.setColor(new Color(34, 139, 34));
            g.fillRect(0, GAME_HEIGHT - 50, GAME_WIDTH, 50);
            
            // Draw grass details
            g.setColor(new Color(0, 100, 0));
            for (int i = 0; i < GAME_WIDTH; i += 20) {
                g.drawLine(i, GAME_HEIGHT - 50, i + 5, GAME_HEIGHT - 45);
                g.drawLine(i + 10, GAME_HEIGHT - 50, i + 15, GAME_HEIGHT - 42);
            }
            
            // Draw trees and fruits
            for (Tree tree : trees) {
                tree.draw(g);
            }
            
            // Draw cannon
            drawCannon(g);
            
            // Draw trajectory line
            drawTrajectoryLine(g);
            
            // Draw bullets
            g.setColor(Color.RED);
            for (Bullet bullet : bullets) {
                g.fillOval((int)bullet.x - BULLET_SIZE/2, (int)bullet.y - BULLET_SIZE/2, BULLET_SIZE, BULLET_SIZE);
                
                // Add bullet trail effect
                g.setColor(new Color(255, 255, 0, 100));
                g.fillOval((int)bullet.x - BULLET_SIZE, (int)bullet.y - BULLET_SIZE, BULLET_SIZE * 2, BULLET_SIZE * 2);
                g.setColor(Color.RED);
            }
            
            // Draw falling fruits
            for (FallingFruit fruit : fallingFruits) {
                fruit.draw(g);
            }
            
            // Draw UI
            drawUI(g);
            
        } else {
            // Game over or level complete screen
            if (fruitsRemaining == 0) {
                drawLevelComplete(g);
            } else {
                drawGameOver(g);
            }
        }
    }
    
    private void drawCannon(Graphics2D g) {
        // Save the current transform
        g.translate(cannonX + CANNON_WIDTH/2, cannonY + CANNON_HEIGHT/2);
        g.rotate(cannonAngle);
        
        // Draw cannon base
        g.setColor(new Color(139, 69, 19));
        g.fillOval(-CANNON_WIDTH/2, -CANNON_HEIGHT/2, CANNON_WIDTH, CANNON_HEIGHT);
        
        // Draw cannon barrel
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, -8, 60, 16);
        
        // Draw cannon tip
        g.setColor(Color.BLACK);
        g.fillOval(55, -5, 10, 10);
        
        // Reset transform
        g.rotate(-cannonAngle);
        g.translate(-(cannonX + CANNON_WIDTH/2), -(cannonY + CANNON_HEIGHT/2));
    }
    
    private void drawTrajectoryLine(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
        
        double startX = cannonX + CANNON_WIDTH/2 + Math.cos(cannonAngle) * 60;
        double startY = cannonY + CANNON_HEIGHT/2 + Math.sin(cannonAngle) * 60;
        
        // Draw trajectory points
        for (int i = 0; i < 20; i++) {
            double t = i * 0.1;
            double x = startX + Math.cos(cannonAngle) * 200 * t;
            double y = startY + Math.sin(cannonAngle) * 200 * t + 0.5 * 150 * t * t; // Gravity effect
            
            if (x > GAME_WIDTH || y > GAME_HEIGHT - 50) break;
            
            g.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
        
        g.setStroke(new BasicStroke(1));
    }
    
    private void drawUI(Graphics2D g) {
        // UI Background
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(10, 10, 250, 120);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Level: " + level, 20, 35);
        g.drawString("Score: " + score, 20, 60);
        g.drawString("Ammo: " + ammo, 20, 85);
        g.drawString("Fruits Left: " + fruitsRemaining, 20, 110);
        
        // Wind indicator
        g.drawString("Wind: ", 20, 135);
        if (windForce > 0) {
            g.setColor(Color.CYAN);
            g.drawString("→ " + String.format("%.1f", windForce), 80, 135);
        } else if (windForce < 0) {
            g.setColor(Color.CYAN);
            g.drawString("← " + String.format("%.1f", Math.abs(windForce)), 80, 135);
        } else {
            g.setColor(Color.GREEN);
            g.drawString("Calm", 80, 135);
        }
        
        // Instructions
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Aim with mouse, Click to shoot", GAME_WIDTH - 200, 25);
        g.drawString("Space: Reset level", GAME_WIDTH - 200, 45);
    }
    
    private void drawLevelComplete(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        
        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        String text = "LEVEL " + level + " COMPLETE!";
        g.drawString(text, (GAME_WIDTH - fm.stringWidth(text)) / 2, GAME_HEIGHT / 2 - 50);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g.getFontMetrics();
        String scoreText = "Score: " + score;
        g.drawString(scoreText, (GAME_WIDTH - fm.stringWidth(scoreText)) / 2, GAME_HEIGHT / 2);
        
        String nextText = "Press N for Next Level";
        g.drawString(nextText, (GAME_WIDTH - fm.stringWidth(nextText)) / 2, GAME_HEIGHT / 2 + 50);
    }
    
    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        String text = "GAME OVER";
        g.drawString(text, (GAME_WIDTH - fm.stringWidth(text)) / 2, GAME_HEIGHT / 2 - 50);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g.getFontMetrics();
        String reason = ammo <= 0 ? "Out of Ammo!" : "Try Again!";
        g.drawString(reason, (GAME_WIDTH - fm.stringWidth(reason)) / 2, GAME_HEIGHT / 2);
        
        String restartText = "Press R to Restart";
        g.drawString(restartText, (GAME_WIDTH - fm.stringWidth(restartText)) / 2, GAME_HEIGHT / 2 + 50);
    }
    
    public void update() {
        if (!gameRunning) return;
        
        // Update wind
        if (System.currentTimeMillis() - windChangeTime > 3000) {
            windForce = (random.nextDouble() - 0.5) * 4; // -2 to 2
            windChangeTime = System.currentTimeMillis();
        }
        
        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(windForce);
            
            // Remove bullets that are off screen
            if (bullet.x < 0 || bullet.x > GAME_WIDTH || bullet.y > GAME_HEIGHT - 50) {
                bullets.remove(i);
                continue;
            }
            
            // Check collision with fruits on trees
            for (Tree tree : trees) {
                for (int j = tree.fruits.size() - 1; j >= 0; j--) {
                    TreeFruit fruit = tree.fruits.get(j);
                    if (bullet.collidesWith(fruit)) {
                        // Hit fruit!
                        bullets.remove(i);
                        tree.fruits.remove(j);
                        fallingFruits.add(new FallingFruit(fruit.x, fruit.y, fruit.type));
                        score += fruit.getPoints();
                        fruitsRemaining--;
                        break;
                    }
                }
            }
        }
        
        // Update falling fruits
        for (int i = fallingFruits.size() - 1; i >= 0; i--) {
            FallingFruit fruit = fallingFruits.get(i);
            fruit.update();
            
            if (fruit.y > GAME_HEIGHT) {
                fallingFruits.remove(i);
            }
        }
        
        // Check win/lose conditions
        if (fruitsRemaining == 0) {
            gameRunning = false; // Level complete
        } else if (ammo <= 0 && bullets.isEmpty()) {
            gameRunning = false; // Game over
        }
    }
    
    private void shoot() {
        if (ammo > 0 && System.currentTimeMillis() - lastShot > 300) {
            double startX = cannonX + CANNON_WIDTH/2 + Math.cos(cannonAngle) * 60;
            double startY = cannonY + CANNON_HEIGHT/2 + Math.sin(cannonAngle) * 60;
            
            bullets.add(new Bullet((int)startX, (int)startY, cannonAngle));
            ammo--;
            lastShot = System.currentTimeMillis();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        
        // Calculate cannon angle
        double dx = mouseX - (cannonX + CANNON_WIDTH/2);
        double dy = mouseY - (cannonY + CANNON_HEIGHT/2);
        cannonAngle = Math.atan2(dy, dx);
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (gameRunning) {
            shoot();
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (key == KeyEvent.VK_SPACE && gameRunning) {
            initializeLevel(); // Reset current level
        } else if (key == KeyEvent.VK_R && !gameRunning && fruitsRemaining > 0) {
            // Restart game
            level = 1;
            score = 0;
            gameRunning = true;
            initializeLevel();
        } else if (key == KeyEvent.VK_N && !gameRunning && fruitsRemaining == 0) {
            // Next level
            level++;
            gameRunning = true;
            initializeLevel();
        }
    }
    
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    
    // Game object classes
    class Tree {
        int x, y;
        ArrayList<TreeFruit> fruits;
        
        public Tree(int x, int y) {
            this.x = x;
            this.y = y;
            this.fruits = new ArrayList<>();
        }
        
        public void addFruit(TreeFruit fruit) {
            fruits.add(fruit);
        }
        
        public void draw(Graphics2D g) {
            // Draw trunk
            g.setColor(new Color(101, 67, 33));
            g.fillRect(x + TREE_WIDTH/2 - 15, y + TREE_HEIGHT - 50, 30, 50);
            
            // Draw tree crown
            g.setColor(new Color(34, 139, 34));
            g.fillOval(x, y, TREE_WIDTH, TREE_HEIGHT - 30);
            
            // Add tree texture
            g.setColor(new Color(0, 100, 0));
            for (int i = 0; i < 5; i++) {
                int circleX = x + 20 + (i * 15);
                int circleY = y + 20 + (i * 10);
                g.fillOval(circleX, circleY, 15, 15);
            }
            
            // Draw fruits
            for (TreeFruit fruit : fruits) {
                fruit.draw(g);
            }
        }
    }
    
    class TreeFruit {
        int x, y;
        FruitType type;
        Color color;
        
        public TreeFruit(int x, int y) {
            this.x = x;
            this.y = y;
            
            FruitType[] types = FruitType.values();
            this.type = types[random.nextInt(types.length)];
            
            switch (type) {
                case APPLE: color = Color.RED; break;
                case ORANGE: color = Color.ORANGE; break;
                case CHERRY: color = Color.MAGENTA; break;
                case PEAR: color = Color.GREEN; break;
            }
        }
        
        public int getPoints() {
            switch (type) {
                case APPLE: return 10;
                case ORANGE: return 15;
                case CHERRY: return 20;
                case PEAR: return 25;
                default: return 10;
            }
        }
        
        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval(x, y, FRUIT_SIZE, FRUIT_SIZE);
            
            // Add highlight
            g.setColor(Color.WHITE);
            g.fillOval(x + 3, y + 3, 8, 8);
            
            // Draw stem
            g.setColor(new Color(101, 67, 33));
            g.fillRect(x + FRUIT_SIZE/2 - 1, y - 5, 2, 5);
        }
    }
    
    class FallingFruit {
        double x, y;
        double velocityX, velocityY;
        FruitType type;
        Color color;
        double rotation;
        double rotationSpeed;
        
        public FallingFruit(int startX, int startY, FruitType type) {
            this.x = startX;
            this.y = startY;
            this.type = type;
            this.velocityX = (random.nextDouble() - 0.5) * 4;
            this.velocityY = 2;
            this.rotation = 0;
            this.rotationSpeed = (random.nextDouble() - 0.5) * 0.3;
            
            switch (type) {
                case APPLE: color = Color.RED; break;
                case ORANGE: color = Color.ORANGE; break;
                case CHERRY: color = Color.MAGENTA; break;
                case PEAR: color = Color.GREEN; break;
            }
        }
        
        public void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.2; // Gravity
            rotation += rotationSpeed;
        }
        
        public void draw(Graphics2D g) {
            g.translate((int)x, (int)y);
            g.rotate(rotation);
            
            g.setColor(color);
            g.fillOval(-FRUIT_SIZE/2, -FRUIT_SIZE/2, FRUIT_SIZE, FRUIT_SIZE);
            
            g.setColor(Color.WHITE);
            g.fillOval(-FRUIT_SIZE/2 + 3, -FRUIT_SIZE/2 + 3, 8, 8);
            
            g.rotate(-rotation);
            g.translate(-(int)x, -(int)y);
        }
    }
    
    class Bullet {
        double x, y;
        double velocityX, velocityY;
        
        public Bullet(int startX, int startY, double angle) {
            this.x = startX;
            this.y = startY;
            double power = 15;
            this.velocityX = Math.cos(angle) * power;
            this.velocityY = Math.sin(angle) * power;
        }
        
        public void update(double windForce) {
            x += velocityX + windForce * 0.1;
            y += velocityY;
            velocityY += 0.15; // Gravity
        }
        
        public boolean collidesWith(TreeFruit fruit) {
            double distance = Math.sqrt(Math.pow(x - (fruit.x + FRUIT_SIZE/2), 2) + 
                                      Math.pow(y - (fruit.y + FRUIT_SIZE/2), 2));
            return distance < (FRUIT_SIZE/2 + BULLET_SIZE/2);
        }
    }
    
    enum FruitType {
        APPLE, ORANGE, CHERRY, PEAR
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Aim the Fruit from Tree");
        AimFruitFromTreeGame game = new AimFruitFromTreeGame();
        
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}