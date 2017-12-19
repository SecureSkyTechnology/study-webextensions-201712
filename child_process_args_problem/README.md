- Windows上でのchild_process.execSync()などで、JSONのような `" , ' , \` が混在する文字列を正しく渡せるか検証したコード。
- 結論として、child_process 内部やWindows内部と思われる影響により `" , ' , \` が勝手に削除されたり空白で勝手に分割されるなど予想できない副作用・現象が多発した。
- そのため、JSON文字列をchild_processでコマンドライン引数として渡すのは諦めた。


