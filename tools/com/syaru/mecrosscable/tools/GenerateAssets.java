package com.syaru.mecrosscable.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.syaru.mecrosscable.CableGeometry;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public final class GenerateAssets {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Path root;
    private static final String ID = "me_cross_cable";
    private static final String[] FACES = {"down","up","north","south","west","east"};

    public static void main(String[] args) throws Exception {
        root = Path.of(args[0]);
        texture("shell", -1); texture("port", -2); texture("lamp", -3);
        texture("jacket", 0);
        texture("glow", -4);
        String[] ids = {"cross_cable", "smart_cross_cable", "dense_smart_cross_cable"};
        String[] cables = {"fluix_glass_cable", "fluix_smart_cable", "fluix_smart_dense_cable"};
        for (int variant=0;variant<3;variant++) {
            String id=ids[variant]; boolean dense=variant==2, smart=variant>0;
            var multipart=new ArrayList<Object>();
            multipart.add(Map.of("apply",Map.of("model",ID+":block/"+id)));
            for(int port=0;port<6;port++) {
                String arm=id+"_"+FACES[port];
                multipart.add(Map.of("when",Map.of(FACES[port],"true"),"apply",Map.of("model",ID+":block/"+arm)));
                var armElements=new ArrayList<Object>();
                armElements.add(element(CableGeometry.connector(port,dense),"jacket",3+port));
                for(var b:CableGeometry.connectedCollar(port,dense))armElements.add(element(b,"port",-1));
                for(var light:CableGeometry.portLights(dense))if(light.port()==port)armElements.add(emission(light));
                json("assets/"+ID+"/models/block/"+arm+".json",Map.of("parent","minecraft:block/block",
                        "textures",Map.of("jacket",ID+":block/jacket","port",ID+":block/port","glow",ID+":block/glow"),"elements",armElements));
            }
            json("assets/"+ID+"/blockstates/"+id+".json",Map.of("multipart",multipart));
            model(id, dense, smart);
            json("assets/"+ID+"/models/item/"+id+".json", Map.of("parent",ID+":block/"+id));
            json("data/"+ID+"/recipe/"+id+".json", Map.of("type","minecraft:crafting_shaped",
                    "pattern",List.of(" C ","CQC"), "key",Map.of("C",Map.of("item","ae2:"+cables[variant]),
                    "Q",Map.of("item","ae2:quartz_glass")), "result",Map.of("id",ID+":"+id,"count",1)));
            json("data/"+ID+"/advancement/recipes/"+id+".json", Map.of("parent","minecraft:recipes/root",
                    "criteria", Map.of("has_cable",Map.of("trigger","minecraft:inventory_changed",
                            "conditions",Map.of("items",List.of(Map.of("items",List.of("ae2:"+cables[variant])))))),
                    "rewards",Map.of("recipes",List.of(ID+":"+id))));
            json("data/"+ID+"/loot_table/blocks/"+id+".json", Map.of("type","minecraft:block",
                    "pools",List.of(Map.of("rolls",1,"entries",List.of(Map.of("type","minecraft:item","name",ID+":"+id,
                            "functions",List.of(Map.of("function","minecraft:copy_components","source","block_entity",
                                    "include",List.of(ID+":configuration"))))),
                            "conditions",List.of(Map.of("condition","minecraft:survives_explosion"))))));
        }
        json("data/minecraft/tags/block/mineable/pickaxe.json", Map.of("replace",false,"values",List.of(
                ID+":cross_cable", ID+":smart_cross_cable", ID+":dense_smart_cross_cable")));
        language(false); language(true);
        json("pack.mcmeta", Map.of("pack",Map.of("pack_format",34,"description","ME Cross Cable resources")));
    }

    private static void model(String id, boolean dense, boolean smart) throws Exception {
        var elements=new ArrayList<Object>();
        var textures=new LinkedHashMap<String,String>();
        for (String name:List.of("shell","port","lamp","jacket")) textures.put(name, ID+":block/"+name);
        textures.put("particle",ID+":block/shell");
        for(var b:CableGeometry.housing(dense))elements.add(element(b,"shell",-1));
        for(var b:CableGeometry.frame(dense))elements.add(element(b,"port",-1));
        for(int port=0;port<6;port++) {
            elements.add(element(CableGeometry.socket(port,dense),"lamp",-1));
            for(var b:CableGeometry.collar(port,dense))elements.add(element(b,"port",-1));
            for(var b:CableGeometry.sleeve(port,dense))elements.add(element(b,"jacket",3+port));
        }
        if(smart)for(var lamp:CableGeometry.lamps(dense))elements.add(element(lamp.box(),"lamp",-1,FACES[lamp.port()]));
        var model=new LinkedHashMap<String,Object>();
        model.put("parent","minecraft:block/block"); model.put("ambientocclusion",false);
        model.put("textures",textures); model.put("elements",elements);
        model.put("display",Map.of("gui",Map.of("rotation",List.of(25,225,0),"translation",List.of(0,0,0),"scale",List.of(0.8,0.8,0.8))));
        json("assets/"+ID+"/models/block/"+id+".json",model);
    }

    private static Object element(CableGeometry.Box b, String texture, int tint) {
        return element(b,texture,tint,null);
    }

    private static Object emission(CableGeometry.PortLight light) {
        var b=light.box();
        return Map.of("from",List.of(b.x0(),b.y0(),b.z0()),"to",List.of(b.x1(),b.y1(),b.z1()),"shade",false,
                "faces",Map.of(FACES[light.port()],Map.of("texture","#glow","tintindex",3+light.port(),
                        "uv",List.of(0,0,16,16),"neoforge_data",Map.of("block_light",15,"sky_light",15,"ambient_occlusion",false))));
    }
    private static Object element(CableGeometry.Box b,String texture,int tint,String outwardFace) {
        var faces=new LinkedHashMap<String,Object>();
        for (String side:FACES) {
            if(outwardFace!=null&&!side.equals(outwardFace))continue;
            var face=new LinkedHashMap<String,Object>(); face.put("texture","#"+texture);
            face.put("uv",List.of(0,0,16,16));
            if (tint>=0) face.put("tintindex",tint); faces.put(side,face);
        }
        return Map.of("from",List.of(b.x0(),b.y0(),b.z0()),"to",List.of(b.x1(),b.y1(),b.z1()),"faces",faces);
    }

    private static void texture(String name,int lane) throws Exception {
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
            int color;
            if(lane==-4) color=0xFFFFFF;
            else if(lane==-3) color=0x1B232C;
            else if(lane==-2) color=(x==0||x==15||y==0||y==15)?0x4B4D52:(x<3||y<3)?0xA4A3A8:(x>12||y>12)?0x5F6268:((x/4+y/4)%2==0?0x797B80:0x85868C);
            else if(lane==-1) color=(x==0||y==0)?0xD0D1D4:(x==15||y==15)?0xB7BAC1:((x/4+y/4)%2==0?0xEEEEEE:0xE3E4E7);
            else {
                color=x<2||x>13?0x777777:x<5||x>10?0xC5C5C5:0xFFFFFF;
                if(y<2||y>13) color=0xA0A0A0;
            }
            image.setRGB(x,y,0xFF000000|color);
        }
        Path path=root.resolve("assets/"+ID+"/textures/block/"+name+".png");
        Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());
    }

    private static void language(boolean ja) throws Exception {
        var text=new LinkedHashMap<String,String>();
        text.put("itemGroup.me_cross_cable", "ME Cross Cable");
        text.put("block.me_cross_cable.cross_cable",ja?"MEクロスケーブル":"ME Cross Cable");
        text.put("block.me_cross_cable.smart_cross_cable",ja?"MEスマートクロスケーブル":"ME Smart Cross Cable");
        text.put("block.me_cross_cable.dense_smart_cross_cable",ja?"ME高密度スマートクロスケーブル":"ME Dense Smart Cross Cable");
        text.put("tooltip.me_cross_cable.axes",ja?"接続したケーブルの色で見分ける独立3回線":"Three independent lines, colored by connected cables");
        text.put("tooltip.me_cross_cable.normal",ja?"各回線8チャンネル（AE2標準設定）":"8 channels per line (default AE2 mode)");
        text.put("tooltip.me_cross_cable.dense",ja?"各回線32チャンネル（AE2標準設定）":"32 channels per line (default AE2 mode)");
        text.put("tooltip.me_cross_cable.dye",ja?"右クリックで接続面設定 / 染料で回線を着色":"Right-click: routing / dye: recolor the selected line");
        for(int i=0;i<3;i++)text.put("axis.me_cross_cable."+i,(ja?"回線":"Line ")+(i+1));
        text.put("message.me_cross_cable.status","%s | %s | %s / %s");
        text.put("message.me_cross_cable.unlimited",ja?"無制限":"Unlimited");
        text.put("menu.me_cross_cable",ja?"MEクロスケーブル":"ME Cross Cable");
        text.put("menu.me_cross_cable.pair",ja?"つなぐ面を2つ選択":"Select two faces to connect");
        text.put("menu.me_cross_cable.selected",ja?"%s → 次の面を選択":"%s: select the other face");
        text.put("menu.me_cross_cable.reroute",ja?"残りの接続面は自動で組み替えます":"Displaced ports are paired automatically");
        text.put("menu.me_cross_cable.done",ja?"閉じる":"Done");
        String[] jp={"下","上","北","南","西","東"};
        for(int i=0;i<6;i++)text.put("port.me_cross_cable."+i,ja?jp[i]:FACES[i]);
        json("assets/"+ID+"/lang/"+(ja?"ja_jp":"en_us")+".json",text);
    }
    private static void json(String name,Object value) throws Exception {
        Path path=root.resolve(name);Files.createDirectories(path.getParent());Files.writeString(path,JSON.toJson(value)+"\n");
    }
}
