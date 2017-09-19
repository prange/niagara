package org.kantega.niagara.example;

import org.kantega.niagara.Source;

public class Example2_fold {


    public static void main(String[] args) {

        Source<Long> dbSource =
          new SyncFakeDb()
          .foldLeft(0L,(count,str)->count + 1);




    }


}
