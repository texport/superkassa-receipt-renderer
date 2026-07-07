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
            url: "https://github.com/texport/superkassa-receipt-renderer/releases/download/v1.0.4/SuperkassaReceiptRenderer.xcframework.zip",
            checksum: "815d464aa211a897f185324e85500afa0b24df26af7b51dab50fe7853facc4b5"
        )
    ]
)
