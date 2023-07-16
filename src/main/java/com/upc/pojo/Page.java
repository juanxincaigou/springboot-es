package com.upc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page {

    private String content;

    private int pageNumber;

    private List<ImagePosition> imgPositions;

    private List<String> pageImagesContents;
}
