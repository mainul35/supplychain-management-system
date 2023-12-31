package com.mainul35.bsuserinfo.entity;

import lombok.*;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@EqualsAndHashCode
public class UserConnectionId implements Serializable {
    @Serial
    private static final long serialVersionUID = -5259789512124211493L;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "connection_id")
    private UserEntity connection;
}
