package com.upc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagePosition {
    //使用x、y、width和height来定位图片位置,PDF文件中的坐标系原点在左上角
    //x和y分别表示图片左上角的横坐标和纵坐标,width和height分别表示图片的宽度和高度

    private float x;

    private float y;

    private  float width;

    private  float height;
}
