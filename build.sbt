import ScalateKeys._
import com.mojolly.scalate.ScalatePlugin._
 
seq(scalateSettings:_*)
 
scalateTemplateConfig in Compile <<= (sourceDirectory in Compile) ( base =>
  Seq(
    TemplateConfig(
      base / "app/views",
      Seq(
        // "import template helpers"
      ),
      Seq(
        // Binding("name", "org.package.Class", importMembers=true, isImplicit=true, defaultValue=null),
      ),
      Some("views")
    )
  )
)
 
