package edu.utn.frsf.isi.dan.reservas_svc.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

  /** De 1 a 5. */
  private double rating;

  private String comment;
  private Instant createdAt;
}
