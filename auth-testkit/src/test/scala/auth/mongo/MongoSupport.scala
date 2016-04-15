package auth.mongo

import de.flapdoodle.embed.mongo.config._
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{ Command, MongodStarter }
import de.flapdoodle.embed.process.config.IRuntimeConfig
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.extract.UUIDTempNaming
import de.flapdoodle.embed.process.io.directories.PlatformTempDir
import de.flapdoodle.embed.process.io.{ NullProcessor, Processors }
import de.flapdoodle.embed.process.runtime.Network

trait MongoSupport {
  lazy val host = "localhost"
  lazy val port = Network.getFreeServerPort
  lazy val localHostIPV6 = Network.localhostIsIPv6()

  val mongouri = s"mongodb://$host:$port/test"

  val artifactStorePath = new PlatformTempDir()
  val executableNaming = new UUIDTempNaming()
  val command = Command.MongoD
  val version = Version.Main.V3_2
  val processOutput = new ProcessOutput(
    Processors.named("[mongod>]", new NullProcessor()),
    Processors.named("[MONGOD>]", new NullProcessor()),
    Processors.named("[console>]", new NullProcessor())
  )

  val runtimeConfig: IRuntimeConfig =
    new RuntimeConfigBuilder()
      .defaults(command)
      .processOutput(processOutput)
      .artifactStore(new ExtractedArtifactStoreBuilder()
        .defaults(command)
        .download(new DownloadConfigBuilder()
          .defaultsForCommand(command)
          .artifactStorePath(artifactStorePath))
        .executableNaming(executableNaming))
      .build()

  val mongodConfig =
    new MongodConfigBuilder()
      .version(version)
      .net(new Net(port, localHostIPV6))
      .cmdOptions(new MongoCmdOptionsBuilder()
        .syncDelay(1)
        .useNoPrealloc(false)
        .enableTextSearch(true)
        .build())
      .build()

  lazy val mongodStarter = MongodStarter.getInstance(runtimeConfig)
  lazy val mongod = mongodStarter.prepare(mongodConfig)
  val mongodExe = mongod.start()

  def embeddedMongoStartup() {
    println("(going to launch embedded mongo)")
    mongodExe
    println("(embedded mongo launched)")
  }

  def embeddedMongoShutdown() {
    mongod.stop()
    mongodExe.stop()
  }
}