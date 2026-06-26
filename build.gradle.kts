import org.jooq.meta.jaxb.ForcedType
import java.io.FileReader
import kotlin.apply

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jooq.jooq-codegen-gradle") version "3.19.25"
}

group = "personal.jinhyeok"
version = "0.0.1-SNAPSHOT"
description = "tasklog_planner_backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("tools.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.postgresql:postgresql")
    jooqCodegen("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("compileJava") {
    dependsOn("jooqCodegen")
}

tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

val jooqSourceDir = "build/generated-sources/jooq"

sourceSets {
    main {
        java {
            srcDir(jooqSourceDir)
        }
    }
}

val environmentPath = project.rootProject.file("gradle/.env")
val envMap: MutableMap<String, String> = LinkedHashMap()

if (environmentPath.exists()) {
    FileReader(environmentPath).use { reader ->
        reader.forEachLine { line ->
            run {
                if (line.isBlank() || line.startsWith(("#")) || !line.contains("=")) {
                    return@forEachLine
                }
                val (key, value) = line.split("=", limit = 2)
                envMap[key] = value
            }
        }
    }
}

fun jooqProp(key: String, default: String): String =
    System.getenv(key) ?: envMap[key] ?: default

val jooqJdbcUrl = jooqProp("DATASOURCE_URL", "jdbc:postgresql://localhost:5432/auth_service")
val jooqUser = jooqProp("DATASOURCE_USERNAME", "postgres")
val jooqPassword = jooqProp("DATASOURCE_PASSWORD", "postgres")
val params = parseQueryParams(jooqJdbcUrl)
fun parseQueryParams(jdbcUrl: String): Map<String, String> {
    val query = jdbcUrl.substringAfter("?", "")
    if (query.isBlank()) return emptyMap()

    return query.split("&")
        .mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size != 2) return@mapNotNull null

            val key = parts[0].trim()
            val value = parts[1].trim()
            key to value
        }
        .toMap()
}

jooq {
    configuration {
        logging = org.jooq.meta.jaxb.Logging.WARN

        jdbc {
            driver = "org.postgresql.Driver"
            url = jooqJdbcUrl
            user = jooqUser
            password = jooqPassword
        }
        generator {
            generate {
                immutablePojos = true
                pojosAsJavaRecordClasses = true
            }
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = params["currentSchema"] ?: "public"
                excludes = listOf(
                    // PostGIS metadata tables/views
                    "spatial_ref_sys",
                    "geometry_columns",
                    "geography_columns",
                    "raster_columns",
                    "raster_overviews",
                    // PostGIS table-valued functions
                    "postgis_.*",
                    // PostGIS SQL functions
                    "st_.*",
                    "_st_.*",
                    "geometry.*",
                    "geography.*",
                    "addgeometrycolumn",
                    "dropgeometrycolumn",
                    "dropgeometrytable",
                    "find_srid",
                    "populate_geometry_columns",
                    "updategeometrysrid",
                    "checkauth",
                    "lockrow",
                    "unlockrows",
                    "gettransactionid",
                    "enablelongtransactions",
                    "disablelongtransactions",
                    "box.*",
                    "equals",
                    "contains.*"
                ).joinToString("|")
                forcedTypes = listOf(
                    ForcedType().apply {
                        name = "timestamp(p) with time zone"
                        includeTypes = "TIMESTAMP|TIMESTAMPTZ"
                    },
                    ForcedType().apply {
                        name = "time(p) with time zone"
                        includeTypes = "TIME"
                    }
                )
            }
            target {
                packageName = "personal.jinhyeok.jooq"
                directory = jooqSourceDir
            }
        }
    }
}
