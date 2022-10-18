package net.cydhra.acromantula.gx

import net.cydhra.acromantula.features.archives.ArchiveFeature
import net.cydhra.acromantula.features.exporter.ExporterFeature
import net.cydhra.acromantula.features.importer.ImporterFeature
import net.cydhra.acromantula.features.view.GenerateViewFeature
import net.cydhra.acromantula.plugins.AcromantulaPlugin
import org.apache.logging.log4j.LogManager

class GxPlugin : AcromantulaPlugin {

    companion object {
        private val logger = LogManager.getLogger()
    }

    override val author: String = "Cydhra"

    override val name: String = "CT3 GX Parsers"

    override fun initialize() {
        ArchiveFeature.registerArchiveType(ArtArchiveType)
        ImporterFeature.registerImporterStrategy(ArtArchiveImporterStrategy())
        ExporterFeature.registerExporterStrategy(ArtArchiveExporterStrategy())
        GenerateViewFeature.registerViewGenerator(GxtxViewGenerator())
        logger.info("registered gx parsers")
    }

}