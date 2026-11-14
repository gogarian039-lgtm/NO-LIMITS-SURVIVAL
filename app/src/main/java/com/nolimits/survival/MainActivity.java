package com.nolimits.survival;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
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
        int wood = 0, stone = 0;
        final float[][] resources = {{2.5f,2.5f,0},{7.5f,3.5f,0},{10.5f,4.5f,1},{4.5f,7.5f,1},{12.5f,9.5f,0},{8.5f,12.5f,1},{3.5f,12.5f,0}};
        final boolean[] collected = new boolean[7];
        String notice = "";
        long noticeUntil = 0;
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
        int nearestResource() {
            int best=-1; float distance=1.25f;
            for(int i=0;i<resources.length;i++) {
                if(collected[i])continue;
                float dx=resources[i][0]-px,dy=resources[i][1]-py;
                float d=(float)Math.sqrt(dx*dx+dy*dy);
                if(d<distance){distance=d;best=i;}
            }
            return best;
        }
        void collect() {
            int i=nearestResource();
            if(i<0){notice="Move closer to a resource";noticeUntil=System.currentTimeMillis()+1500;return;}
            collected[i]=true;
            if(resources[i][2]==0){wood++;notice="+1 WOOD";}else{stone++;notice="+1 STONE";}
            noticeUntil=System.currentTimeMillis()+1500;
        }
        float dp(float value) { return value * getResources().getDisplayMetrics().density; }
        float hudMargin() { return dp(14); }
        float buttonWidth() { return Math.min(dp(116), getWidth()*0.29f); }
        float buttonHeight() { return dp(56); }
        float pickupLeft() { return getWidth()-hudMargin()-buttonWidth(); }
        float pickupTop() { return getHeight()-Math.max(dp(110),getHeight()*0.13f)-buttonHeight(); }
        float cameraLeft() { return getWidth()-hudMargin()-buttonWidth(); }
        float cameraTop() { return hudMargin(); }
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
            // Draw resource markers with simple perspective, from farthest to nearest.
            for(int pass=0;pass<resources.length;pass++) {
                int best=-1;double far=-1;
                for(int i=0;i<resources.length;i++) {
                    if(collected[i])continue;
                    double dx=resources[i][0]-px,dy=resources[i][1]-py;
                    double d=dx*dx+dy*dy;
                    // Each pass uses a distance threshold to draw far objects first.
                    int farther=0;
                    for(int j=0;j<resources.length;j++)if(!collected[j]) {
                        double jx=resources[j][0]-px,jy=resources[j][1]-py;
                        if(jx*jx+jy*jy>d)farther++;
                    }
                    if(farther==pass){best=i;far=d;break;}
                }
                if(best<0)continue;
                double dx=resources[best][0]-px,dy=resources[best][1]-py;
                double depth=dx*Math.cos(angle)+dy*Math.sin(angle);
                double side=-dx*Math.sin(angle)+dy*Math.cos(angle);
                if(depth<0.3||depth>18)continue;
                double projected=side/(depth*Math.tan(fov/2));
                if(Math.abs(projected)>1.15)continue;
                // Occlude markers if a wall is between the player and resource.
                boolean hidden=false;
                double dist=Math.sqrt(far);
                for(double d=0.12;d<dist-0.15;d+=0.08)
                    if(blocked((float)(px+dx*d/dist),(float)(py+dy*d/dist))){hidden=true;break;}
                if(hidden)continue;
                float screenX=(float)(w*(0.5+projected*0.5));
                float size=(float)Math.min(h*0.32,h*0.37/depth);
                float bottom=horizon+size*0.8f;
                if(resources[best][2]==0) {
                    fill(c,0xff76512c,screenX-size*0.10f,bottom-size*0.6f,screenX+size*0.10f,bottom);
                    circle(c,0xff246c32,screenX,bottom-size*0.75f,size*0.34f);
                    circle(c,0xff358343,screenX-size*0.17f,bottom-size*0.65f,size*0.26f);
                } else {
                    circle(c,0xff777f88,screenX,bottom-size*0.16f,size*0.27f);
                    circle(c,0xffa6abb0,screenX-size*0.07f,bottom-size*0.24f,size*0.14f);
                }
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
            float base=Math.min(w*0.17f,h*0.105f);
            float stickCx=base+hudMargin(),stickCy=h-base-hudMargin()-dp(22);
            circle(c,0x66404040,stickCx,stickCy,base);
            circle(c,0x99ffffff,stickCx+stickX*base*0.55f,
                   stickCy+stickY*base*0.55f,base*0.36f);
            float left=hudMargin(), top=hudMargin();
            float titleSize=Math.min(dp(18),w*0.042f);
            float smallSize=Math.min(dp(15),w*0.036f);
            fill(c,0x990d2518,left-dp(5),top-dp(5),Math.min(w*0.60f,dp(265)),top+dp(74));
            label(c,"NO LIMITS SURVIVAL",left,top+dp(20),titleSize);
            label(c,"WOOD: "+wood+"   STONE: "+stone,left,top+dp(48),smallSize);
            if(System.currentTimeMillis()<noticeUntil)
                label(c,notice,left,top+dp(97),smallSize);
            fill(c,0xaa303c44,cameraLeft(),cameraTop(),w-hudMargin(),cameraTop()+buttonHeight());
            label(c,thirdPerson?"CAM: 3RD":"CAM: 1ST",cameraLeft()+dp(9),
                  cameraTop()+buttonHeight()*0.60f,smallSize);
            fill(c,0xbb245d34,pickupLeft(),pickupTop(),w-hudMargin(),pickupTop()+buttonHeight());
            label(c,"PICK UP",pickupLeft()+dp(12),pickupTop()+buttonHeight()*0.62f,smallSize);
            if(nearestResource()>=0)
                label(c,"Resource nearby!",left,h-dp(52),smallSize);
            postInvalidateDelayed(16);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked(),index=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN) {
                int id=e.getPointerId(index),x=(int)e.getX(index),y=(int)e.getY(index);
                if(x>=pickupLeft()&&x<=getWidth()-hudMargin()&&y>=pickupTop()&&y<=pickupTop()+buttonHeight()) {collect();invalidate();return true;}
                if(x>=cameraLeft()&&x<=getWidth()-hudMargin()&&y>=cameraTop()&&y<=cameraTop()+buttonHeight()) {
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
                    float base=Math.min(getWidth()*0.17f,getHeight()*0.105f);
                    float cx=base+hudMargin(),cy=getHeight()-base-hudMargin()-dp(22);
                    stickX=Math.max(-1,Math.min(1,(e.getX(i)-cx)/base));
                    stickY=Math.max(-1,Math.min(1,(e.getY(i)-cy)/base));
                } else if(id==lookId) {
                    lookX=Math.max(-1,Math.min(1,(e.getX(i)-getWidth()*0.75f)/(getWidth()*0.23f)));
                    lookY=Math.max(-1,Math.min(1,(e.getY(i)-getHeight()*0.5f)/(getHeight()*0.5f)));
                }
            }
            invalidate();return true;
        }
    }
}
