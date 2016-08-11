package com.sjcdigital.temis.model.service.bots.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sjcdigital.temis.model.exceptions.BotException;
import com.sjcdigital.temis.model.service.bots.AbstractBot;
import com.sjcdigital.temis.util.File;

/**
 *
 * @author pedro-hos
 *
 *         Classe responsável por pegar as páginas das leis em: http://www.ceaam.net/sjc/legislacao/index.php, e salvá-las em uma pasta para realização de parse posteriormente.
 *
 */
@Component
public class LawsBot extends AbstractBot {

	private static final Logger LOGGER = LogManager.getLogger(LawsBot.class);

	@Value("${url.laws}")
	private String lawsUrl;

	@Value("${path.leis}")
	private String path;

	@Autowired
	private File file;

	public void savePages() throws BotException {

		final LocalTime start = LocalTime.now();
		final List<Integer> allYears = getAllYears();

		String code = "L8865"; // TODO: Recuperar do banco, caso ja exista dado
		String url = "";
		String body = "";

		boolean tryNextYear = false;

		final int currentYear = LocalDate.now().getYear();
		int index = 0;
		int limitToTry = 10;
		Integer year = null;

		while (index != allYears.size()) {

			try {

				year = allYears.get(index);
				url = buildURL(year, code);
				body = getPage(url).get().html();

				LOGGER.info("READ URL: " + url);

				file.createFile(getPath(), body, code, year);
				code = buildLawCode(getNextLawCode(code));
				tryNextYear = false;
				limitToTry = 10;

			} catch (InterruptedException | ExecutionException | IOException exception) {

				if (exception instanceof FileNotFoundException) {

					LOGGER.error("Error 404: " + url);

					if (year != currentYear && !tryNextYear) {
						index++;
						tryNextYear = true;

					} else {

						if (limitToTry == 0) {
							break;
						}

						if (year != currentYear) {
							index--;
						}

						limitToTry--;
						code = buildLawCode(getNextLawCode(code));
					}

				} else {
					LOGGER.error(ExceptionUtils.getStackTrace(exception));
					throw new BotException(exception);
				}

			}

		}

		LOGGER.info("ELAPSED TIME: " + Duration.between(start, LocalTime.now()));

	}

	private List<Integer> getAllYears() {

		final List<Integer> years = new LinkedList<>();

		int year = 2013;
		final int currentYear = LocalDate.now().getYear();

		final boolean dataBaseEmpty = true;

		if (!dataBaseEmpty) {
			years.add(currentYear);
		}

		while (year <= currentYear) {
			years.add(year);
			year += 1;
		}

		return years;

	}

	private String buildURL(final Integer year, final String code) {
		return lawsUrl.concat(year.toString()).concat("/").concat(code).concat(".htm");
	}

	private BigInteger getNextLawCode(final String current) {
		final BigInteger nextLawCode = new BigInteger(current.replace("L", "")).add(BigInteger.ONE);
		return nextLawCode;
	}

	private String buildLawCode(final BigInteger code) {
		return "L" + StringUtils.leftPad(code.toString(), 4, "0");
	}

	@Override
	protected String getPath() {
		return path.concat("leis/");
	}

}
