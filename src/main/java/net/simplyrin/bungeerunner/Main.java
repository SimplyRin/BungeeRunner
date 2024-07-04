package net.simplyrin.bungeerunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Created by SimplyRin on 2021/02/15.
 *
 * Copyright (c) 2021 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class Main {

	public static void main(String[] args) {
		new Main(args).run();
	}
	
	private final String[] args;
	
	public Main(String[] args) {
		this.args = args;
	}
	
	private String url = "https://ci.md-5.net";
	private String job = "BungeeCord";

	private String stableBuildNumber = null;

	private final File buildNumber = new File("StableBuildNumber.txt");
	private File bungeeCord;

	public void run() {
		if (this.args.length > 0) {
			this.url = this.args[0];
		}
		
		if (this.args.length > 1) {
			this.job = this.args[1];
		}
		
		this.bungeeCord = new File(this.job + ".jar");
		
		System.out.println("ダウンロードサーバー: " + this.url);
		System.out.println("ジョブ名: " + this.job);

		if (!this.buildNumber.exists()) {
			System.out.println("最終安定ビルドの番号ファイルを作成しています...。");
			try {
				this.buildNumber.createNewFile();

				if (this.url.contains("github")) {
					this.stableBuildNumber = this.getStableBuildNumberGitHub();
				} else {
					this.stableBuildNumber = this.getStableBuildNumber();
				}
				FileWriter fileWriter = new FileWriter(this.buildNumber);
				fileWriter.write(this.stableBuildNumber);
				fileWriter.close();
				System.out.println("最終安定ビルドの番号ファイルを作成しました。");
			} catch (Exception e) {
				System.err.println("最終安定ビルドの番号ファイルの作成に失敗しました。");
				e.printStackTrace();
			}
		}

		if (!this.bungeeCord.exists()) {
			System.out.println(this.bungeeCord.getName() + " をダウンロードしています...。");
			this.downloadJar(this.bungeeCord, this.stableBuildNumber);
			System.out.println(this.bungeeCord.getName() + " のダウンロードが完了しました。");
		}

		if (this.url.contains("github")) {
			this.stableBuildNumber = this.getStableBuildNumberGitHub();
		} else {
			this.stableBuildNumber = this.getStableBuildNumber();
		}
		String dlNumber = this.getDownloadedBuildNumber();
		if (dlNumber == null) {
			System.err.println("最終安定ビルド番号の取得に失敗しました。");
		} else if (!this.stableBuildNumber.equals(dlNumber)) {
			System.out.println(this.job + " のアップデートを確認しました。");
			File target = new File(this.job + "-v" + dlNumber + ".jar");
			this.bungeeCord.renameTo(target);
			System.out.println(this.bungeeCord.getName() + " を " + target.getName() + " に変更しました。");

			if (this.downloadJar(this.bungeeCord, this.stableBuildNumber)) {
				this.updateDownloadedBuildNumber();
				System.out.println(this.bungeeCord.getName() + " のダウンロードが完了しました。");
			} else {
				System.err.println(this.bungeeCord.getName() + " のダウンロードに失敗しました。");
				target.renameTo(this.bungeeCord);
				System.err.println(target.getName() + " を " + this.bungeeCord.getName() + " に変更しました。");
			}
		} else {
			System.out.println("最新の " + this.job + " を使用しています。v" + this.getDownloadedBuildNumber());
		}
	}

	public String getStableBuildNumber() {
		String url = this.url + "/job/" + this.job + "/lastStableBuild/buildNumber";
		try {
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
			connection.connect();
			Scanner scanner = new Scanner(connection.getInputStream());
			String buildNumber = scanner.nextLine();
			scanner.close();
			return buildNumber;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getStableBuildNumberGitHub() {
		String url = this.url.replace("https://github.com/", "https://api.github.com/repos/") + "/releases";
		try {
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
			connection.connect();

			String result = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);

			JsonObject json = JsonParser.parseString( result ).getAsJsonArray().get( 0 ).getAsJsonObject();

			String buildNumber = json.get( "tag_name" ).getAsString().replace( "v", "" );

			return buildNumber;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getDownloadedBuildNumber() {
		try {
			FileReader fileReader = new FileReader(this.buildNumber);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String buildNumber = bufferedReader.readLine();
			bufferedReader.close();
			return buildNumber;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean updateDownloadedBuildNumber() {
		try {
			FileWriter fileWriter = new FileWriter(this.buildNumber);

			if (this.url.contains("github")) {
				this.stableBuildNumber = this.getStableBuildNumberGitHub();
			} else {
				this.stableBuildNumber = this.getStableBuildNumber();
			}

			fileWriter.write(this.stableBuildNumber);
			fileWriter.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean downloadJar(File file, String stableBuildNumber) {
		try {
			String url;
			if (this.url.contains("github")) {
				url = this.url + "/releases/download/v" + stableBuildNumber + "/BungeeCord.jar";
			} else {
				url = this.url + "/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar";
			}
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36");
			connection.connect();
			FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
