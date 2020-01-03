package com.vivasoft.vaka_device_manager.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by IntelliJ IDEA.
 * User: Md. Shamim
 * Date: ৭/১১/১৯
 * Time: ৭:৩৬ PM
 * Email: mdshamim723@gmail.com
 **/

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResponseTemplate<T> {
  private boolean success;
  private T data;
  private String message;
}
