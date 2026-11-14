package com.nolimits.survival;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new WorldView());
    }

    // Lightweight first-person 3D raycaster; no external engine or assets required.
    final class WorldView extends View {
        final Paint paint = new Paint(3);
        final String[] map = {
            "1111111111111111",
            "1000000000000001",
            "1000000000000001",
            "1000011000000001",
            "1000010000000001",
            "1000000000000001",
            "1000000001100001",
            "1000000001000001",
            "1000000000000001",
            "1001100000000001",
            "1000000000000001",
            "1000000000011001",
            "1000000000000001",
            "1000000000000001",
            "1000000000000001",
            "1111111111111111"
        };
        float px = 3.5f, py = 3.5f, angle = 0.35f;
        float stickX, stickY, lookX, lookY;
        int stickId = -1, lookId = -1;
        boolean thirdPerson = false;
        long previous = System.nanoTime();
        WorldView() { super(MainActivity.this); setFocusable(true); }

        boolean blocked(float x, float y) {
            int mx = (int)x, my = (int)y;
            return my < 0 || my >= map.length || mx < 0 ||
                   mx >= map[0].length() || map[my].charAt(mx) == '1';
        }
        void move(float dx, float dy) {
            float radius = 0.18f;
            if (!blocked(px + dx + Math.signum(dx)*radius, py)) px += dx;
            if (!blocked(px, py + dy + Math.signum(dy)*radius)) py += dy;
        }
        void fill(Canvas c, int color, float l, float t, float r, float b) {
            paint.setColor(color); paint.setStyle(Paint.Style.FILL);
            c.drawRect(l,t,r,b,paint);
        }
        void circle(Canvas c, int color, float x,float y,float radius) {
            paint.setColor(color); paint.setStyle(Paint.Style.FILL);
            c.drawCircle(x,y,radius,paint);
        }
        void label(Canvas c,String text,float x,float y,float size) {
            paint.setColor(Color.WHITE); paint.setTextSize(size);
            paint.setShadowLayer(3,1,2,Color.BLACK);
            c.drawText(text,x,y,paint); paint.clearShadowLayer();
        }
        @Override protected void onDraw(Canvas c) {
            int w=getWidth(),h=getHeight();
            if(w==0||h==0)return;
            long now=System.nanoTime();
            float dt=Math.min(0.05f,(now-previous)/1000000000f);
            previous=now;
            // Left thumbstick: forward/back and strafe. Right half: look.
            float forward=-stickY, strafe=stickX;
            float speed=2.8f*dt;
            move((float)(Math.cos(angle)*forward-Math.sin(angle)*strafe)*speed,
                 (float)(Math.sin(angle)*forward+Math.cos(angle)*strafe)*speed);
            angle += lookX*dt*1.9f;
            float horizon=h*0.48f+lookY*h*0.12f;
            fill(c,0xff79b9e6,0,0,w,horizon);
            fill(c,0xff527e48,0,horizon,w,h);
            float fov=(float)Math.toRadians(66);
            int step=Math.max(2,w/240);
            for(int x=0;x<w;x+=step) {
                double ray=angle+Math.atan((2.0*x/w-1.0)*Math.tan(fov/2));
                double rx=Math.cos(ray),ry=Math.sin(ray);
                double distance=0.03;
                boolean hit=false;
                while(distance<22) {
                    if(blocked((float)(px+rx*distance),(float)(py+ry*distance))) {
                        hit=true;break;
                    }
                    distance+=0.035;
                }
                if(!hit)continue;
                double corrected=Math.max(0.12,distance*Math.cos(ray-angle));
                float height=(float)(h*0.73/corrected);
                int shade=(int)Math.max(55,205-distance*13);
                int color=Color.rgb((int)(shade*0.62),(int)(shade*0.68),(int)(shade*0.57));
                fill(c,color,x,horizon-height/2,x+step+1,horizon+height/2);
            }
            // Crosshair and optional third-person avatar overlay.
            float cx=w/2f,cy=horizon;
            fill(c,Color.WHITE,cx-9,cy-1,cx+9,cy+1);
            fill(c,Color.WHITE,cx-1,cy-9,cx+1,cy+9);
            if(thirdPerson) {
                circle(c,0xffe7c19d,cx, h*0.67f,w*0.045f);
                fill(c,0xff1a405c,cx-w*0.055f,h*0.71f,cx+w*0.055f,h*0.87f);
                fill(c,0xff26313b,cx-w*0.045f,h*0.87f,cx-w*0.008f,h*0.99f);
                fill(c,0xff26313b,cx+w*0.008f,h*0.87f,cx+w*0.045f,h*0.99f);
            }
            float base=Math.min(w,h)*0.14f;
            circle(c,0x66404040,base*1.3f,h-base*1.35f,base);
            circle(c,0x99ffffff,base*1.3f+stickX*base*0.55f,
                   h-base*1.35f+stickY*base*0.55f,base*0.36f);
            fill(c,0x99505050,w-155,24,w-16,82);
            label(c,thirdPerson?"CAM: 3RD":"CAM: 1ST",w-147,62,Math.max(14,w*0.035f));
            label(c,"NO LIMITS SURVIVAL",18,44,Math.max(19,w*0.05f));
            label(c,"Explore the world  |  Left: move  Right: look",18,h-20,Math.max(12,w*0.027f));
            postInvalidateDelayed(16);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),index=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
                int id=e.getPointerId(index),x=(int)e.getX(index),y=(int)e.getY(index);
                if(x>getWidth()-170&&y<100) {
                    thirdPerson=!thirdPerson;invalidate();return true;
                }
                if(x<getWidth()*0.5f&&stickId==-1)stickId=id;
                else if(lookId==-1)lookId=id;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP||
               action==MotionEvent.ACTION_CANCEL) {
                int id=e.getPointerId(index);
                if(id==stickId||action==MotionEvent.ACTION_CANCEL) {
                    stickId=-1;stickX=stickY=0;
                }
                if(id==lookId||action==MotionEvent.ACTION_CANCEL) {
                    lookId=-1;lookX=lookY=0;
                }
            }
            for(int i=0;i<e.getPointerCount();i++) {
                int id=e.getPointerId(i);
                if(id==stickId) {
                    float base=Math.min(getWidth(),getHeight())*0.14f;
                    stickX=Math.max(-1,Math.min(1,(e.getX(i)-base*1.3f)/base));
                    stickY=Math.max(-1,Math.min(1,(e.getY(i)-(getHeight()-base*1.35f))/base));
                } else if(id==lookId) {
                    lookX=Math.max(-1,Math.min(1,(e.getX(i)-getWidth()*0.75f)/(getWidth()*0.23f)));
                    lookY=Math.max(-1,Math.min(1,(e.getY(i)-getHeight()*0.5f)/(getHeight()*0.5f)));
                }
            }
            invalidate();return true;
        }
    }
}
