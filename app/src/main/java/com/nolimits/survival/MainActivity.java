package com.nolimits.survival;
import android.app.*;import android.os.*;import android.view.*;import android.graphics.*;import java.util.*;
public class MainActivity extends Activity {
 Game game; public void onCreate(Bundle b){super.onCreate(b);game=new Game();setContentView(game);}
 class Game extends View {
 Paint p=new Paint(3); Random r=new Random(); float x=400,y=900,enemyX=200,enemyY=100;int hp=100,score=0,level=1;boolean playing=true;long last=System.currentTimeMillis();
 Game(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
 void rect(Canvas c,int color,float a,float b,float d,float e){p.setColor(color);c.drawRect(a,b,d,e,p);}
 void txt(Canvas c,String s,int size,float a,float b){p.setColor(Color.WHITE);p.setTextSize(size);c.drawText(s,a,b,p);}
 protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(); c.drawColor(Color.rgb(8,16,30));float sx=w/800f,sy=h/1200f;c.save();c.scale(sx,sy);
 for(int i=0;i<12;i++){rect(c,0xff18283d,(i*113)%800,80+i*94,(i*113)%800+60,95+i*94);}
 txt(c,"NO LIMITS: SURVIVAL",40,35,65);txt(c,"HP "+hp+"    SCORE "+score+"    LEVEL "+level,28,35,115);
 if(playing){long now=System.currentTimeMillis();if(now-last>35){enemyY+=7+level;last=now;if(enemyY>1100){enemyY=130;enemyX=70+r.nextInt(650);hp-=15;if(hp<=0)playing=false;}}
 rect(c,0xff00d9ff,x-32,y-32,x+32,y+32);rect(c,0xffff4455,enemyX-30,enemyY-30,enemyX+30,enemyY+30);
 txt(c,"DRAG TO MOVE  /  TAP ENEMY TO SHOOT",23,30,1160);
 }else{txt(c,"GAME OVER",70,190,550);txt(c,"TAP TO RESTART",35,240,630);}
 c.restore();postInvalidateDelayed(35);}
 public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==1||e.getAction()==2){float tx=e.getX()*800/getWidth(),ty=e.getY()*1200/getHeight();if(!playing){hp=100;score=0;level=1;playing=true;enemyY=100;}else if(Math.abs(tx-enemyX)<90&&Math.abs(ty-enemyY)<90){score+=10;level=1+score/100;enemyX=60+r.nextInt(680);enemyY=100;}else{x=Math.max(35,Math.min(765,tx));y=Math.max(160,Math.min(1050,ty));}return true;}return true;}
 }
}