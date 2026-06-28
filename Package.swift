// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "SuperkassaReceiptRenderer",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SuperkassaReceiptRenderer",
            targets: ["SuperkassaReceiptRenderer"]
        ),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "SuperkassaReceiptRenderer",
            url: "https://github.com/texport/superkassa-receipt-renderer/releases/download/v1.0.2/SuperkassaReceiptRenderer.xcframework.zip",
            checksum: "bdd6d218e249f2874860dce230ad0a78b91854a99498b818371bdd10e5705a10"
        )
    ]
)
