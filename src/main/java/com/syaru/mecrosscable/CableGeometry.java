package com.syaru.mecrosscable;

import java.util.ArrayList;
import java.util.List;

/** Shared by baked models, collision and channel rendering; coordinates are model pixels. */
public final class CableGeometry {
    public record Box(double x0,double y0,double z0,double x1,double y1,double z1) { }
    public record Lamp(int port,int segment,Box box) { }
    public record PortLight(int port,Box box) { }

    private CableGeometry() { }

    public static double cableWidth(boolean dense) { return dense?10:6; }
    public static double connectorWidth(boolean dense) { return dense?8:4; }
    public static double bodyWidth(boolean dense) { return cableWidth(dense)+3; }
    public static double bodyInset(boolean dense) { return (16-bodyWidth(dense))/2; }
    public static double socketWidth(boolean dense) { return dense?4.4:2.4; }

    public static List<Box> housing(boolean dense) {
        double inset=bodyInset(dense)+0.6,opening=(16-socketWidth(dense))/2;
        double[] cut={inset,opening,16-opening,16-inset};
        List<Box> boxes=new ArrayList<>();
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++) {
            int centered=(x==1?1:0)+(y==1?1:0)+(z==1?1:0);
            // Leave six real socket recesses rather than drawing holes on a solid cube.
            if(centered==2)continue;
            boxes.add(new Box(cut[x],cut[y],cut[z],cut[x+1],cut[y+1],cut[z+1]));
        }
        return List.copyOf(boxes);
    }

    public static List<Box> frame(boolean dense) {
        List<Box> boxes=new ArrayList<>();
        double inset=bodyInset(dense)+0.55,width=0.5;
        for(double x:new double[]{inset,16-inset-width})for(double z:new double[]{inset,16-inset-width})
            boxes.add(new Box(x,inset,z,x+width,16-inset,z+width));
        return List.copyOf(boxes);
    }

    public static List<Box> collar(int port,boolean dense) {
        double inset=bodyInset(dense);
        // Recess the shell, so raised sockets still fit the complete 9/13-pixel envelope.
        return ring(port,(16-socketWidth(dense))/2-0.7,0.7,inset,inset+0.65);
    }

    public static List<Box> connectedCollar(int port,boolean dense) {
        double inset=bodyInset(dense);
        return ring(port,(16-connectorWidth(dense))/2-1,1,inset,inset+0.65);
    }

    public static List<Box> sleeve(int port,boolean dense) {
        double inset=bodyInset(dense);
        return ring(port,(16-socketWidth(dense))/2,0.12,inset+0.2,inset+0.56);
    }

    public static Box socket(int port,boolean dense) {
        double lo=(16-socketWidth(dense))/2,inset=bodyInset(dense);
        return faceBox(port,lo,lo,16-lo,16-lo,inset+0.55,inset+0.57);
    }

    public static Box connector(int port,boolean dense) {
        double lo=(16-connectorWidth(dense))/2;
        return faceBox(port,lo,lo,16-lo,16-lo,0,lo);
    }

    private static List<Box> ring(int port,double lo,double width,double start,double end) {
        double hi=16-lo;
        return List.of(faceBox(port,lo,lo,hi,lo+width,start,end),
                faceBox(port,lo,hi-width,hi,hi,start,end),
                faceBox(port,lo,lo+width,lo+width,hi-width,start,end),
                faceBox(port,hi-width,lo+width,hi,hi-width,start,end));
    }

    public static List<Lamp> lamps(boolean dense) {
        List<Lamp> result=new ArrayList<>();
        int rows=dense?16:8,columns=dense?2:1;
        for(int port=0;port<6;port++)for(int row=0;row<rows;row++)for(int column=0;column<columns;column++) {
            double inset=bodyInset(dense);
            double pitch=0.15,width=dense?0.10:0.20;
        double u=8+connectorWidth(dense)/2+1.10+column*pitch;
            double vertical=0.33,height=0.20;
            double v=8-((rows-1)*vertical+height)/2+row*vertical;
            result.add(new Lamp(port,row*columns+column,
                    faceBox(port,u,v,u+width,v+height,inset+0.57,inset+0.58)));
        }
        return List.copyOf(result);
    }

    public static List<PortLight> portLights(boolean dense) {
        var result=new ArrayList<PortLight>();
        double inset=bodyInset(dense);
        for(int port=0;port<6;port++)for(var box:ring(port,(16-connectorWidth(dense))/2-1,
                1,inset-0.01,inset))result.add(new PortLight(port,box));
        return List.copyOf(result);
    }

    /** Map face-local u/v and inward depth to model coordinates, in Direction ordinal order. */
    private static Box faceBox(int port,double u0,double v0,double u1,double v1,double d0,double d1) {
        return switch(port) {
            case 0 -> new Box(u0,d0,v0,u1,d1,v1);
            case 1 -> new Box(u0,16-d1,v0,u1,16-d0,v1);
            case 2 -> new Box(u0,v0,d0,u1,v1,d1);
            case 3 -> new Box(u0,v0,16-d1,u1,v1,16-d0);
            case 4 -> new Box(d0,v0,u0,d1,v1,u1);
            case 5 -> new Box(16-d1,v0,u0,16-d0,v1,u1);
            default -> throw new IllegalArgumentException("Invalid face: "+port);
        };
    }
}
