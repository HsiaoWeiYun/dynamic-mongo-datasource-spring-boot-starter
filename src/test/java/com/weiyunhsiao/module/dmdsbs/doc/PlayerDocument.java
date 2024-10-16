package com.weiyunhsiao.module.dmdsbs.doc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "player")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class PlayerDocument {
    private String id;

    @NonNull
    private String name;
}
