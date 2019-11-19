import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
public class BacteriveDriver extends JFrame
{
   private BacterivePanel gamePanel=new BacterivePanel();
   private boolean running=true,paused=false;
   private int ups,fps;
   private BacteriveDriver()
   {
      super("Bacterive");
      pack();
      setSize(800,800);
      setLocationRelativeTo(null);
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setContentPane(gamePanel);
      setVisible(true);
      gamePanel.requestFocus();
      runGameLoop();
   }
   private void runGameLoop()
   {
      Thread game=
         new Thread()
         {
            @Override
            public void run()
            {
               gameLoop();
            }
         };
      game.run();
   }
   private void gameLoop()
   {
      final int TARGET_UPS=30;
      final long TARGET_UPDATE_TIME=1000000000/TARGET_UPS;
      final int TARGET_FPS=60;
      final long TARGET_FRAME_TIME=1000000000/TARGET_FPS;
      long prevFrameTime=System.nanoTime();
      long accumulator=0;
      //for finding ups
      int updates=0;
      long runningUpdateTime=0;
      long prevUpdateTime=prevFrameTime;
      //for finding fps
      int frames=0;
      long runningFrameTime=0;
      while(running)
      {
         long now=System.nanoTime();
         accumulator+=now-prevFrameTime;
         runningFrameTime+=now-prevFrameTime;
         frames++;
         while(accumulator>TARGET_UPDATE_TIME)
         {
            gamePanel.update();
            accumulator-=TARGET_UPDATE_TIME;
            runningUpdateTime+=System.nanoTime()-prevUpdateTime;
            updates++;
            prevUpdateTime=System.nanoTime();
         }
         double interpolation=(double)(System.nanoTime()-prevFrameTime)/TARGET_UPDATE_TIME;
         //System.out.println(interpolation);
         if(paused)
            interpolation=0;
         if(runningUpdateTime>=1000000000)
         {
            ups=updates;
            updates=0;
            runningUpdateTime=0;
         }
         if(runningFrameTime>=1000000000)
         {
            fps=frames;
            frames=0;
            runningFrameTime=0;
         }
         gamePanel.render(interpolation);
         prevFrameTime=now;
         while(now-prevFrameTime<TARGET_FRAME_TIME && now-prevFrameTime<TARGET_UPDATE_TIME)
         {
            Thread.yield();
            now=System.nanoTime();
         }
      }
   }
   public static void main(String[]arg)
   {
      new BacteriveDriver();
   }
   private class BacterivePanel extends JPanel
   {
      private ArrayList<Bacterium> bacteria=new ArrayList<Bacterium>(0);
      private int fieldX=-750,fieldY=-1000,fieldWidth=1500,fieldHeight=2000;
      private int MAX_BACTERIA=50;
      private final String MOVE_UP="move up"
                           ,MOVE_DOWN="move down"
                           ,MOVE_LEFT="move left"
                           ,MOVE_RIGHT="move right"
                           ,STOP_VERTICAL="stop vertical"
                           ,STOP_HORIZONTAL="stop horizontal"
                           ,EXIT="exit"
                           ,PAUSE="pause";
      private BufferedImage tile;
      private BacterivePanel()
      {
         try{tile=ImageIO.read(new File("Sprites/BackgroundTile.png"));} 
         catch(IOException e){e.printStackTrace();}
         initBacteria();
         setBackground(Color.WHITE);
         setKeyBindings();
         setFocusable(true);
      }
      private void initBacteria()
      {
         bacteria.add(new Player());
         for(int i=0;i<100;i++)
         {
            Enemy temp=new Enemy((int)(Math.random()*(fieldWidth+1))+fieldX,(int)(Math.random()*(fieldHeight+1))+fieldY,bacteria.get(0).size);
            while(isCollision(temp,bacteria.get(0)))
            {
               temp.x+=100;
               temp.y+=100;
            }
            bacteria.add(temp);
         }
      }
      private void gimp(String key,String name)
      {
         getInputMap().put(KeyStroke.getKeyStroke(key),name);
      }
      private void gamp(String name,AbstractAction action)
      {
         getActionMap().put(name,action);
      }
      private void setKeyBindings()
      {
         gimp("UP",MOVE_UP);
         gimp("W",MOVE_UP);
         gimp("DOWN",MOVE_DOWN);
         gimp("S",MOVE_DOWN);
         gimp("LEFT",MOVE_LEFT);
         gimp("A",MOVE_LEFT);
         gimp("RIGHT",MOVE_RIGHT);
         gimp("D",MOVE_RIGHT);
         gimp("ESCAPE",EXIT);
         gimp("P",PAUSE);
         
         gimp("released UP",STOP_VERTICAL);
         gimp("released W",STOP_VERTICAL);
         gimp("released DOWN",STOP_VERTICAL);
         gimp("released S",STOP_VERTICAL);
         gimp("released LEFT",STOP_HORIZONTAL);
         gimp("released A",STOP_HORIZONTAL);
         gimp("released RIGHT",STOP_HORIZONTAL);
         gimp("released D",STOP_HORIZONTAL);
         
         gamp(MOVE_UP,new MoveAction(0));
         gamp(MOVE_RIGHT,new MoveAction(1));
         gamp(MOVE_DOWN,new MoveAction(2));
         gamp(MOVE_LEFT,new MoveAction(3));
         gamp(EXIT,new MenuAction(0));
         gamp(PAUSE,new MenuAction(1));
         
         gamp(STOP_VERTICAL,new MoveAction(4));
         gamp(STOP_HORIZONTAL,new MoveAction(5));
      }
      private void detectCollisions()
      {
         for(int i=0;i<bacteria.size();i++)
         {
            Bacterium b1=bacteria.get(i);
            for(int j=i+1;j<bacteria.size();j++)
            {
               Bacterium b2=bacteria.get(j);
               if(isCollision(b1,b2))
                  if(b1.size>b2.size)
                  {
                     b1.size+=b2.size/10;
                     //b1.maxSpeed++;
                     b1.x-=b2.size/20;
                     b1.y-=b2.size/20;
                     bacteria.remove(b2);
                  }
                  else if(b1 instanceof Player && b1.size!=b2.size)
                  {
                     running=false;
                  }
                  else if(b1.size<b2.size)
                  {
                     b2.size+=b1.size/10;
                     //b2.maxSpeed++;
                     b2.x-=b1.size/20;
                     b2.y-=b1.size/20;
                     bacteria.remove(b1);
                  }
            }
         }
      }
      private boolean isCollision(Bacterium b1,Bacterium b2)
      {
         return Math.sqrt(Math.pow(b2.getCenterX()-b1.getCenterX(),2)+Math.pow(b2.getCenterY()-b1.getCenterY(),2))<b1.size/2+b2.size/2;
      }
      private void spawnEnemies()
      {
         MAX_BACTERIA=fieldHeight;
         if(bacteria.size()<MAX_BACTERIA)
         {
            Enemy temp=new Enemy((int)(Math.random()*(fieldWidth+1))+fieldX,(int)(Math.random()*(fieldHeight+1))+fieldY,bacteria.get(0).size);
            while(isCollision(temp,bacteria.get(0)))
            {
               temp.x+=100;
               temp.y+=100;
            }
            bacteria.add(temp);
         }
      }
      private void update()
      {
         for(Bacterium bacterium:bacteria)
         {
            int targetSize=bacterium.size*5;
            if(targetSize>fieldWidth)
            {
               fieldWidth=targetSize;
               fieldHeight=targetSize;
            }
         }
         if(bacteria.get(0).size>500)
            for(Bacterium bacterium:bacteria)
               bacterium.size/=2;
         spawnEnemies();
         for(Bacterium bacterium:bacteria)
            bacterium.update(fieldX,fieldY,fieldWidth,fieldHeight);
         detectCollisions();
      }
      private void render(double interpolation)
      {
         for(Bacterium bacterium:bacteria)
            bacterium.interpolate(interpolation);
         repaint();
      }
      @Override
      public void paintComponent(Graphics g)
      {
         super.paintComponent(g);
         Bacterium p=bacteria.get(0);
         int tileWidth=tile.getWidth();
         int tileHeight=tile.getHeight();
         for(int x=-tileWidth;x<getWidth()+tileWidth;x+=tileWidth)
            for(int y=-tileHeight;y<getHeight()+tileHeight;y+=tileWidth)
               g.drawImage(tile,x-p.getCenterX()%tileWidth,y-p.getCenterY()%tileHeight,tileWidth,tileHeight,null);
         int translateX=getWidth()/2-p.getCenterX();
         int translateY=getHeight()/2-p.getCenterY();
         g.translate(translateX,translateY);
         for(Bacterium bacterium:bacteria)
            bacterium.draw(g);
         
         g.setColor(Color.BLACK);
         g.drawRect(fieldX,fieldY,fieldWidth,fieldHeight);
         g.translate(-translateX,-translateY);
         
         g.setFont(new Font(Font.DIALOG_INPUT,Font.BOLD,20));
         g.drawString("UPS: "+ups,getWidth()-100,getHeight()-50);
         g.drawString("FPS: "+fps,getWidth()-100,getHeight()-25);
         g.setFont(new Font(Font.DIALOG_INPUT,Font.BOLD,30));
         g.drawString("Size: "+p.size,20,getHeight()-25);
         if(!running)
         {
            g.setColor(Color.RED);
            g.setFont(new Font(Font.DIALOG_INPUT,Font.BOLD,75));
            g.drawString("YOU LOSE",getWidth()/2-175,100);
         }
         else if(paused)
         {
            g.setColor(Color.RED);
            g.setFont(new Font(Font.DIALOG_INPUT,Font.BOLD,75));
            g.drawString("PAUSED",getWidth()/2-135,100);
         }
      }
      private class MoveAction extends AbstractAction
      {
         private int direction;
         private MoveAction(int dir)
         {
            direction=dir;
         }
         @Override
         public void actionPerformed(ActionEvent e)
         {
            Player p=(Player)bacteria.get(0);
            if(direction<4)
            {
               p.move(direction);
               p.update(fieldX,fieldY,fieldWidth,fieldHeight);
            }
            else
               p.stop(direction);
         }
      }
   }
   private class MenuAction extends AbstractAction
   {
      int code;
      private MenuAction(int key)
      {
         code=key;
      }
      @Override
      public void actionPerformed(ActionEvent e)
      {
         if(code==0)
            System.exit(1);
         else if(code==1)
            paused=!paused;
      }
   }
   private class Bacterium
   {
      protected final int minSpeed;
      protected int x,y,xSpeed,ySpeed,size,maxSpeed;
      private Color color;
      private Bacterium(int xPos,int yPos,int diameter,int minS)
      {
         x=xPos;
         y=yPos;
         size=diameter;
         maxSpeed=(int)(1.0/size*300);
         int rand=(int)(Math.random()*5);
         if(rand==0)
            color=Color.BLUE;
         else if(rand==1)
            color=Color.RED;
         else if(rand==2)
            color=Color.BLACK;
         else if(rand==3)
            color=Color.GREEN;
         else
            color=Color.ORANGE;
         minSpeed=minS;
      }
      private void interpolate(double interpolation)
      {
         x+=xSpeed*interpolation;
         y+=ySpeed*interpolation;
      }
      protected void update(int fX,int fY,int fW,int fH)
      {
         if(this instanceof Player)
            maxSpeed=(int)(1.0/size*500);
         else
            maxSpeed=(int)(1.0/size*300);
         if(maxSpeed<minSpeed)
            maxSpeed=minSpeed;
         if(x+xSpeed<=fX)
         {
            x=fX;
            xSpeed=0;
         }
         else if(x+xSpeed+size>=fX+fW)
         {
            x=fX+fW-size;
            xSpeed=0;
         }
         if(y+ySpeed<=fY)
         {
            y=fY;
            ySpeed=0;
         }
         else if(y+ySpeed+size>=fY+fH)
         {
            y=fY+fH-size;
            ySpeed=0;
         }
      }
      protected void draw(Graphics g)
      {
         g.setColor(color);
         g.fillOval(x,y,size,size);
      }
      private int getCenterX()
      {
         return x+size/2;
      }
      private int getCenterY()
      {
         return y+size/2;
      }
   }
   private class Player extends Bacterium
   {
      private Player()
      {
         super(0,0,50,5);
      }
      private void move(int direction)
      {
         if(direction==0)
            ySpeed=-maxSpeed;
         else if(direction==1)
            xSpeed=maxSpeed;
         else if(direction==2)
            ySpeed=maxSpeed;
         else if(direction==3)
            xSpeed=-maxSpeed;
      }
      private void stop(int direction)
      {
         if(direction==4)
            ySpeed=0;
         else if(direction==5)
            xSpeed=0;
      }
   }
   private class Enemy extends Bacterium
   {
      private Enemy(int xPos,int yPos,int pSize)
      {
         super(xPos,yPos,(int)(Math.random()*pSize/2)+10,2);
      }
      protected void update(int fX,int fY,int fW,int fH)
      {
         int xS=xSpeed;
         int yS=ySpeed;
         super.update(fX,fY,fW,fH);
         if(xS!=xSpeed || yS!=ySpeed || (xSpeed==0 && ySpeed==0))
         {
            int xDir=(int)(Math.random()*3);
            int yDir=(int)(Math.random()*3);
            if(xDir==0)
               xSpeed=0;
            else if(xDir==1)
               xSpeed=-maxSpeed;
            else
               xSpeed=maxSpeed;
            if(yDir==0)
               ySpeed=0;
            else if(yDir==1)
               ySpeed=-maxSpeed;
            else
               ySpeed=maxSpeed;
         }
      }
   }
}