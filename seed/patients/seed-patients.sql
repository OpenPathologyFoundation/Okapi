-- Patient seed data from Xenonym (seed: azure-vale-9728)
-- Source: Starling/seed/patients/xenonym-azure-vale-9728.json
-- Run manually: psql -h localhost -U starling_service -d starling_auth -p 5433 -f Starling/seed/patients/seed-patients.sql
-- Requires: V7__core_patient_schema.sql migration applied first.

BEGIN;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000001', 'Ourfir', 'Bruntilavoul', 'Ourfir Bruntilavoul', '1955-02-15', 'F', '555-810-7415', 'ourfir.bruntilavoul@xenonym.test', '{"line":"3350 Ntakes Road","city":"Weifrer","state":"XM","zip":"90449","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000002', 'Iomithono', 'Ruwupos', 'Iomithono Ruwupos', '2007-02-02', 'F', '555-952-7112', 'iomithono.ruwupos@xenonym.test', '{"line":"6887 Ndathateavav Avenue","city":"Krinhuharem","state":"YP","zip":"90736","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000003', 'Chungum', 'Khumgezvovgaur', 'Chungum Khumgezvovgaur', '1971-08-24', 'F', '555-395-4992', 'chungum.khumgezvovgaur@xenonym.test', '{"line":"3087 Tantarnupo Path","city":"Nimsunorvosol","state":"WK","zip":"90135","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000004', 'Nteapu', 'Fraurvasfetre', 'Nteapu Fraurvasfetre', '1944-09-24', 'X', '555-322-6229', 'nteapu.fraurvasfetre@xenonym.test', '{"line":"1132 Shasmi Avenue","city":"Jenma","state":"QV","zip":"90109","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000005', 'Wrountis', 'Jegiothiotheka', 'Wrountis Jegiothiotheka', '2005-12-04', 'F', '555-541-6286', 'wrountis.jegiothiotheka@xenonym.test', '{"line":"7486 Rakeato Street","city":"Neiquoumendusiav","state":"ZN","zip":"90920","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000006', 'Lourmarauro', 'Quaskozadrim', 'Lourmarauro Quaskozadrim', '1964-04-24', 'F', '555-482-7800', 'lourmarauro.quaskozadrim@xenonym.test', '{"line":"646 Ziju Drive","city":"Wrosrundoza","state":"XM","zip":"90145","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000007', 'Triojul', 'Nunvesnu', 'Triojul Nunvesnu', '2007-12-24', 'F', '555-584-3084', 'triojul.nunvesnu@xenonym.test', '{"line":"1967 Ndeandisur Street","city":"Tralsinser","state":"YP","zip":"90508","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000008', 'Umseth', 'Douwrim', 'Umseth Douwrim', '1999-07-17', 'M', '555-712-4387', 'umseth.douwrim@xenonym.test', '{"line":"7280 Sadaimkon Way","city":"Whohubulkimar","state":"GD","zip":"90577","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000009', 'Eimbabo', 'Graimdauknau', 'Eimbabo Graimdauknau', '1990-01-15', 'F', '555-385-9046', 'eimbabo.graimdauknau@xenonym.test', '{"line":"4309 Hesundiwur Court","city":"Ghuvron","state":"QV","zip":"90009","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000010', 'Piksa', 'Speisleknatuv', 'Piksa Speisleknatuv', '2020-05-25', 'F', '555-551-0760', 'piksa.speisleknatuv@xenonym.test', '{"line":"9868 Ntothejilviv Path","city":"Pobei","state":"FH","zip":"90362","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000011', 'Mbealaiwhil', 'Spusniozrir', 'Mbealaiwhil Spusniozrir', '1979-03-12', 'F', '555-418-6239', 'mbealaiwhil.spusniozrir@xenonym.test', '{"line":"9117 Khouvo Circle","city":"Sleghul","state":"VR","zip":"90810","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000012', 'Vrujil', 'Harkeanjio', 'Vrujil Harkeanjio', '1999-07-10', 'F', '555-639-8253', 'vrujil.harkeanjio@xenonym.test', '{"line":"7017 Wondures Boulevard","city":"Liwhontim","state":"PL","zip":"90699","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000013', 'Zravga', 'Eazrumosdith', 'Zravga Eazrumosdith', '1984-01-11', 'M', '555-967-5630', 'zravga.eazrumosdith@xenonym.test', '{"line":"2967 Pramlivpin Boulevard","city":"Wrikwauv","state":"WK","zip":"90204","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000014', 'Souspos', 'Plukjeinda', 'Souspos Plukjeinda', '1988-11-20', 'X', '555-474-6067', 'souspos.plukjeinda@xenonym.test', '{"line":"3267 Vronpu Boulevard","city":"Juklavruv","state":"QV","zip":"90089","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000015', 'Varniravli', 'Zeispasashan', 'Varniravli Zeispasashan', '2007-04-26', 'M', '555-394-0344', 'varniravli.zeispasashan@xenonym.test', '{"line":"9547 Zendes Avenue","city":"Zhereir","state":"ZN","zip":"90464","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000016', 'Eachosril', 'Wilgol', 'Eachosril Wilgol', '1921-05-21', 'M', '555-162-2061', 'eachosril.wilgol@xenonym.test', '{"line":"9429 Niwhimlai Avenue","city":"Nkurnol","state":"VR","zip":"90551","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000017', 'Aulkolun', 'Lethahutirno', 'Aulkolun Lethahutirno', '1964-07-05', 'F', '555-428-6116', 'aulkolun.lethahutirno@xenonym.test', '{"line":"4122 Jioknoum Road","city":"Wheisasfarveko","state":"QV","zip":"90743","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000018', 'Glilmezair', 'Gusa', 'Glilmezair Gusa', '1980-01-22', 'M', '555-572-2166', 'glilmezair.gusa@xenonym.test', '{"line":"2029 Zimfa Lane","city":"Whinmerveralkebar","state":"FH","zip":"90346","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000019', 'Jewutheveas', 'Bermunirvum', 'Jewutheveas Bermunirvum', '2002-05-27', 'M', '555-688-7107', 'jewutheveas.bermunirvum@xenonym.test', '{"line":"2604 Jisgev Path","city":"Zhonfam","state":"NW","zip":"90144","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000020', 'Khorva', 'Ethabovtor', 'Khorva Ethabovtor', '1990-07-13', 'F', '555-522-5165', 'khorva.ethabovtor@xenonym.test', '{"line":"5015 Letir Avenue","city":"Tarnorvetaurnotoun","state":"YP","zip":"90281","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000021', 'Riamem', 'Doutrakiatar', 'Riamem Doutrakiatar', '1966-04-25', 'F', '555-579-9308', 'riamem.doutrakiatar@xenonym.test', '{"line":"6882 Gejenka Circle","city":"Faurnovutentea","state":"QV","zip":"90166","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000022', 'Quewrin', 'Select', 'Quewrin Select', '1993-04-17', 'F', '555-652-1107', 'quewrin.select@xenonym.test', '{"line":"5857 Nkonjunzin Street","city":"Ngekfav","state":"NW","zip":"90846","country":"Xenonymia"}'::jsonb, true, '{"edge_cases":["edge_case:injection_select"],"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000023', 'Ozi', 'Urnosovlam', 'Ozi Urnosovlam', '1987-07-09', 'M', '555-282-5200', 'ozi.urnosovlam@xenonym.test', '{"line":"8534 Whiathiko Street","city":"Wirhollunlis","state":"WK","zip":"90678","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000024', 'Thisovau', 'Oquuski', 'Thisovau Oquuski', '1967-08-24', 'F', '555-509-6812', 'thisovau.oquuski@xenonym.test', '{"line":"9593 Blirvorokal Boulevard","city":"Sombarnumha","state":"CM","zip":"90611","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000025', 'Jasmol', 'Gilhemin', 'Jasmol Gilhemin', '1979-12-12', 'X', '555-988-0491', 'jasmol.gilhemin@xenonym.test', '{"line":"5041 Pfopouthurail Street","city":"Bendavronreil","state":"GD","zip":"90774","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000026', 'Chuthir', 'Fokhanerjun', 'Chuthir Fokhanerjun', '1993-05-23', 'M', '555-704-4778', 'chuthir.fokhanerjun@xenonym.test', '{"line":"5089 Nugozhim Boulevard","city":"Zvolebiv","state":"PL","zip":"90839","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000027', 'Mbilkupum', 'Select', 'Mbilkupum Select', '2010-06-24', 'F', '555-289-0151', 'mbilkupum.select@xenonym.test', '{"line":"9738 Ntumguprith Place","city":"Moflun","state":"GD","zip":"90590","country":"Xenonymia"}'::jsonb, true, '{"edge_cases":["edge_case:injection_select"],"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000028', 'Faughikho', 'Duzhous', 'Faughikho Duzhous', '2023-02-22', 'X', '555-593-4914', 'faughikho.duzhous@xenonym.test', '{"line":"2406 Karnedarnejil Terrace","city":"Bundorvefa","state":"YP","zip":"90060","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000029', 'Churmin', 'Zrirnolou', 'Churmin Zrirnolou', '1996-07-04', 'F', '555-114-2829', 'churmin.zrirnolou@xenonym.test', '{"line":"6665 Zhagammes Place","city":"Snurnel","state":"VR","zip":"90094","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000030', 'Ngupla', 'Inotos', 'Ngupla Inotos', '1958-01-20', 'M', '555-673-7132', 'ngupla.inotos@xenonym.test', '{"line":"7414 Saiklaingou Path","city":"Svakduwhokhum","state":"PL","zip":"90273","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000031', 'Ogujiam', 'Antulham', 'Ogujiam Antulham', '1957-02-21', 'F', '555-749-8291', 'ogujiam.antulham@xenonym.test', '{"line":"2621 Khousveth Path","city":"Gedethekes","state":"ZN","zip":"90996","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000032', 'Tumbedu', 'Luvainjurzath', 'Tumbedu Luvainjurzath', '2024-06-25', 'M', '555-311-2302', 'tumbedu.luvainjurzath@xenonym.test', '{"line":"3346 Balham Circle","city":"Mbaingemzezho","state":"TL","zip":"90707","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000033', 'Blepiki', 'Ukzensouth', 'Blepiki Ukzensouth', '1920-07-06', 'M', '555-999-0773', 'blepiki.ukzensouth@xenonym.test', '{"line":"1257 Skalva Court","city":"Wequav","state":"KT","zip":"90767","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000034', 'Aurvezaska', 'Imwombai', 'Aurvezaska Imwombai', '1977-06-01', 'M', '555-327-5114', 'aurvezaska.imwombai@xenonym.test', '{"line":"521 Zvendufiawraul Circle","city":"Zithobausno","state":"JR","zip":"90342","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000035', 'Mindouth', 'Wannaksiathove', 'Mindouth Wannaksiathove', '2022-07-21', 'M', '555-842-8320', 'mindouth.wannaksiathove@xenonym.test', '{"line":"9209 Puzeirmipa Road","city":"Daimfear","state":"QV","zip":"90352","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000036', 'Mackheaviorfil', 'Quithigomzaisse', 'Mackheaviorfil Quithigomzaisse', '1992-04-20', 'F', '555-199-7898', 'mackheaviorfil.quithigomzaisse@xenonym.test', '{"line":"1706 Liogouvren Drive","city":"Krifosoun","state":"ZN","zip":"90737","country":"Xenonymia"}'::jsonb, true, '{"edge_cases":["edge_case:mixed_case"],"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000037', 'Tingioth', 'Sinkizvitrath', 'Tingioth Sinkizvitrath', '1974-03-07', 'F', '555-980-7773', 'tingioth.sinkizvitrath@xenonym.test', '{"line":"1978 Wrovwoum Circle","city":"Weimjairmukev","state":"CM","zip":"90919","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000038', 'Hurmekavev', 'Peinvir', 'Hurmekavev Peinvir', '1957-04-22', 'F', '555-403-1369', 'hurmekavev.peinvir@xenonym.test', '{"line":"8525 Sveivithojil Place","city":"Dibrataithedea","state":"PL","zip":"90481","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000039', 'Ghaithiwaukje', 'Vrevrekjauv', 'Ghaithiwaukje Vrevrekjauv', '1934-06-09', 'F', '555-997-1321', 'ghaithiwaukje.vrevrekjauv@xenonym.test', '{"line":"741 Zemlar Place","city":"Pijezu","state":"FH","zip":"90658","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000040', 'Heafendaji', 'Wegloth', 'Heafendaji Wegloth', '2012-10-21', 'X', '555-244-2552', 'heafendaji.wegloth@xenonym.test', '{"line":"3571 Ndegranbuv Path","city":"Snalalkomnas","state":"JR","zip":"90864","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000041', 'Indedas', 'Zhauzrervandim', 'Indedas Zhauzrervandim', '1932-12-26', 'F', '555-482-7683', 'indedas.zhauzrervandim@xenonym.test', '{"line":"2142 Wulfor Street","city":"Knuwir","state":"QV","zip":"90348","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000042', 'Floksapea', 'Blonkammith', 'Floksapea Blonkammith', '1985-11-17', 'X', '555-454-9692', 'floksapea.blonkammith@xenonym.test', '{"line":"3883 Nternipiov Drive","city":"Malbusdusvis","state":"BX","zip":"90496","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000043', 'Hilraisbus', 'Weigenneith', 'Hilraisbus Weigenneith', '1962-08-19', 'M', '555-586-8774', 'hilraisbus.weigenneith@xenonym.test', '{"line":"9265 Vamza Road","city":"Traituthidas","state":"VR","zip":"90237","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000044', 'Lumweis', 'Eneth', 'Lumweis Eneth', '1942-09-09', 'M', '555-261-6857', 'lumweis.eneth@xenonym.test', '{"line":"1278 Whanum Way","city":"Klujes","state":"VR","zip":"90208","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000045', 'Chelkojath', 'Quutru', 'Chelkojath Quutru', '2002-01-14', 'F', '555-856-2667', 'chelkojath.quutru@xenonym.test', '{"line":"8048 Buspauth Street","city":"Snosikni","state":"YP","zip":"90935","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000046', 'Ongeal', 'Drofongoth', 'Ongeal Drofongoth', '1955-03-13', 'F', '555-446-0309', 'ongeal.drofongoth@xenonym.test', '{"line":"8557 Vrernuli Road","city":"Skothaveibum","state":"GD","zip":"90710","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000047', 'Jathe', 'Zaizos', 'Jathe Zaizos', '2000-01-28', 'M', '555-924-4629', 'jathe.zaizos@xenonym.test', '{"line":"8730 Snavpi Street","city":"Mbibrihouvor","state":"NW","zip":"90179","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000048', 'Mikhauthevoth', 'Vrodrautrem', 'Mikhauthevoth Vrodrautrem', '1983-06-25', 'F', '555-863-7445', 'mikhauthevoth.vrodrautrem@xenonym.test', '{"line":"9271 Lamaus Avenue","city":"Zutsiorailo","state":"XM","zip":"90442","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000049', 'Nkosde', 'Tigorvarnosair', 'Nkosde Tigorvarnosair', '2007-04-09', 'M', '555-495-7407', 'nkosde.tigorvarnosair@xenonym.test', '{"line":"642 Tsevo Drive","city":"Drotsuvzipen","state":"XM","zip":"90310","country":"Xenonymia"}'::jsonb, true, '{"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

INSERT INTO core.patients (mrn, given_name, family_name, display_name, dob, sex, phone, email, address, is_test_patient, metadata)
VALUES (
    'XN-000050', 'Khošnèm', 'Úmithãrus', 'Khošnèm Úmithãrus', '2017-06-09', 'F', '555-309-9991', 'khosnem.umitharus@xenonym.test', '{"line":"3965 Jarjarloth Drive","city":"Zvevuknon","state":"CM","zip":"90865","country":"Xenonymia"}'::jsonb, true, '{"edge_cases":["edge_case:diacritic"],"xenonym_seed":"azure-vale-9728"}'::jsonb
)
ON CONFLICT (mrn) DO NOTHING;

COMMIT;

-- Verification
-- SELECT mrn, display_name, sex, dob, is_test_patient FROM core.patients ORDER BY mrn;
