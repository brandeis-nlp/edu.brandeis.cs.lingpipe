def idx = 0
annotations  ( &$ChunkSet.Chunk.foreach {
             [
                      id : "lp${idx++}",
                      start : &.@start.text(),
                      end : &.@end.text(),
                      label : "http://vocab.lappsgrid.org/NamedEntity",
                      features : [
                          category : &.@TYPE.text(),
                          word : &.text()
                      ]
                  ]
              })