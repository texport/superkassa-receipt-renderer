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
            url: "https://github.com/texport/superkassa-receipt-renderer/releases/download/v1.0.3/SuperkassaReceiptRenderer.xcframework.zip",
            checksum: "34b980dbb066a085cedddba05aec7ef4b2a13fd26b97d03f900e336acd308168"
        )
    ]
)
